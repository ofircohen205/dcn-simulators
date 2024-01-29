from os import getcwd, makedirs, listdir, remove
import os.path as osp
from typer import Typer
import pandas as pd

from mlu_lb.schemas.distributed_training import DistributedTraining

app = Typer()

bandwidths = [10, 25, 100, 200, 400]
loads = [0.1, 0.25, 0.5, 0.75, 0.9, 1.0]
link_failure_rates = [0.0, 0.01, 0.05, 0.1]


def read_file(file_name: str) -> dict:
    with open(file_name, "r") as f:
        lines = f.readlines()
        lines = list(map(lambda x: x.replace("\n", ""), lines))[31:]
        delta_t = 0
        delta_ts: dict = {}
        for index, line in enumerate(lines):
            if line.startswith("Crossing virtual links:"):
                delta_ts[delta_t] = {}
                delta_ts[delta_t]["start"] = index
            if (
                delta_t in delta_ts
                and delta_ts[delta_t]["start"]
                and line.startswith("--------------------------------------------------")
            ):
                delta_ts[delta_t]["end"] = index + 1
                delta_t += 1

        flows_delta_t_indices = {}
        jobs_delta_t_indices = {}
        start_flow_index, end_flow_index = -1, -1
        start_job_index, end_job_index = -1, -1
        for delta_t, indices in delta_ts.items():
            start, end = indices["start"], indices["end"]
            delta_t_lines = lines[start:end]
            for line in delta_t_lines:
                if line.startswith("Crossing virtual links:"):
                    start_flow_index = delta_t_lines.index(line) + start + 1
                if line.startswith("Job loggers:"):
                    end_flow_index = delta_t_lines.index(line) + start
                    flows_delta_t_indices[delta_t] = (start_flow_index, end_flow_index)
                    start_job_index = delta_t_lines.index(line) + start + 1
                    break
            end_job_index = end - 1
            jobs_delta_t_indices[delta_t] = (start_job_index, end_job_index)

    final_delta_ts: dict = {}
    for delta_t in flows_delta_t_indices:
        final_delta_ts[delta_t] = {}
        final_delta_ts[delta_t]["flows"] = lines[
            flows_delta_t_indices[delta_t][0] : flows_delta_t_indices[delta_t][1]
        ]
        jobs = lines[jobs_delta_t_indices[delta_t][0] : jobs_delta_t_indices[delta_t][1]]
        jobs_ids = [eval(job.split(",")[0].split("{")[-1].split("=")[-1]) for job in jobs]
        jobs_tors = [eval(job.split("torIds=")[-1].split("]")[0] + "]") for job in jobs]
        final_delta_ts[delta_t]["jobs"] = {
            job_id: job_tors for job_id, job_tors in zip(jobs_ids, jobs_tors)
        }
    return final_delta_ts


def update_links(links: dict, cores: dict, flow: str):
    path_nodes = list(
        map(
            int,
            flow.split("path=")[-1].split("]")[0].replace("[", "").replace(" ", "").split(","),
        )
    )[1:-1]
    for i in range(len(path_nodes) - 1):
        link = str({path_nodes[i], path_nodes[i + 1]})
        if link in links:
            links[link] += 1
        else:
            links[link] = 1

    if path_nodes[1] in cores:
        cores[path_nodes[1]] += 1
    else:
        cores[path_nodes[1]] = 1


def log_flows_stats(
    delta_ts: dict,
    tor_core_links_within_delta_t: dict,
    core_assignments_within_delta_t: dict,
    delta_t: int,
    flows_log: str,
):
    sorted_tor_core_links: list = list(
        sorted(
            tor_core_links_within_delta_t,
            key=lambda x: tor_core_links_within_delta_t.get(x) or -1,
            reverse=True,
        )
    )
    sorted_core_assignments: list = list(
        sorted(
            core_assignments_within_delta_t,
            key=lambda x: core_assignments_within_delta_t.get(x) or -1,
            reverse=True,
        )
    )

    with open(flows_log, "a") as f:
        f.write(f"\u0394t: {delta_t}\n")
        f.write("Job ToRs:\n")
        jobs_tors = delta_ts[delta_t]["jobs"]
        for job_id, job_tors in jobs_tors.items():
            f.write(f"Job {job_id}: {job_tors}\n")
        f.write("Link assignments:\n")
        for link in sorted_tor_core_links:
            f.write(f"{tor_core_links_within_delta_t[link]} assignments on link {link}\n")
        f.write("Core assignments:\n")
        for core in sorted_core_assignments:
            f.write(f"{core_assignments_within_delta_t[core]} assignments on Core {core}\n")
        f.write("-" * 100 + "\n")


def log_job_stats(csv_folder: str, jobs_log: str):
    if osp.exists(jobs_log):
        remove(jobs_log)

    with open(jobs_log, "a") as f:
        for file in sorted(listdir(csv_folder)):
            if file.startswith("job") and file.endswith(".csv"):
                job_id = int(file.split("-")[0].split("_")[-1])
                f.write(f"Job {job_id}:\n")
                df = pd.read_csv(osp.join(csv_folder, file), header=0)
                for epoch, row in df.iterrows():
                    if isinstance(row["epoch_start_time"], str) or isinstance(
                        row["epoch_end_time"], str
                    ):
                        continue
                    f.write(f"\tEpoch {epoch}:\n")
                    start_time = round(row["epoch_start_time"] / 1e9, 2)
                    end_time = round(row["epoch_end_time"] / 1e9, 2)
                    total_time = end_time - start_time
                    f.write(f"\t\tstart time: {start_time}\n")
                    f.write(f"\t\tend time: {end_time}\n")
                    f.write(f"\t\ttotal_time: {total_time}\n")
                f.write("-" * 100 + "\n")


def analyze(
    greedy: bool,
    n_tors: int,
    bw: int,
    load: float,
    parallel: DistributedTraining,
    link_failure_rate: float,
):
    folder = osp.join(
        getcwd(),
        "experiments",
        parallel.value,
        "2_level",
        f"{n_tors}_tors",
        f"bw_{bw}",
        f"load_{load}",
        f"link_failure_rate_{link_failure_rate}",
        "greedy" if greedy else "ecmp",
    )
    makedirs(folder, exist_ok=True)
    file_name = osp.join(folder, "console.txt")
    delta_ts = read_file(file_name=file_name)

    tor_core_links_within_delta_t: dict = {delta_t: dict() for delta_t in delta_ts}
    core_assignments_within_delta_t: dict = {delta_t: dict() for delta_t in delta_ts}
    for delta_t in delta_ts:
        for flow in delta_ts[delta_t]["flows"]:
            update_links(
                links=tor_core_links_within_delta_t[delta_t],
                cores=core_assignments_within_delta_t[delta_t],
                flow=flow,
            )

    analysis_folder = osp.join(folder, "analysis")
    makedirs(analysis_folder, exist_ok=True)

    flows_log = osp.join(analysis_folder, "flows-analysis.log")
    if osp.exists(flows_log):
        remove(flows_log)

    for delta_t in delta_ts:
        log_flows_stats(
            delta_ts=delta_ts,
            tor_core_links_within_delta_t=tor_core_links_within_delta_t[delta_t],
            core_assignments_within_delta_t=core_assignments_within_delta_t[delta_t],
            delta_t=delta_t,
            flows_log=flows_log,
        )
    log_job_stats(csv_folder=folder, jobs_log=osp.join(analysis_folder, "jobs-analysis.log"))


@app.command()
def ecmp_analysis(n_tors: int, parallel: DistributedTraining):
    for link_failure_rate in link_failure_rates:
        for bw in bandwidths:
            for load in loads:
                analyze(
                    greedy=False,
                    n_tors=n_tors,
                    bw=bw,
                    load=load,
                    parallel=parallel,
                    link_failure_rate=link_failure_rate,
                )


@app.command()
def greedy_analysis(
    n_tors: int,
    parallel: DistributedTraining,
):
    for link_failure_rate in link_failure_rates:
        for bw in bandwidths:
            for load in loads:
                analyze(
                    greedy=True,
                    n_tors=n_tors,
                    bw=bw,
                    load=load,
                    parallel=parallel,
                    link_failure_rate=link_failure_rate,
                )


if __name__ == "__main__":
    app()
