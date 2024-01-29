import os
from os import getcwd

import typer

from external.analysis.utils import (
    load_job_info,
    filter_jobs,
    write_job_lines,
    load_flow_info,
    write_flows_metric,
)

app = typer.Typer()

BASE_PATH = os.path.join(getcwd(), "experiments")


@app.command()
def compare_routing_strategies(num_concurrent_jobs: int, num_core_failures: int, ring_size: int):
    experiment_folder = os.path.join(
        BASE_PATH,
        f"concurrent_jobs_{num_concurrent_jobs}",
        f"{num_core_failures}_core_failures",
        f"ring_size_{ring_size}",
    )
    print(f"Experiment Folder: {experiment_folder}")

    dfs = {
        "ecmp": load_job_info(os.path.join(experiment_folder, "ecmp", "logs", "job_info.csv")),
        "mcvlc": load_job_info(os.path.join(experiment_folder, "mcvlc", "logs", "job_info.csv")),
        "edge_coloring": load_job_info(
            os.path.join(experiment_folder, "edge_coloring", "logs", "job_info.csv")
        ),
        "simulated_annealing": load_job_info(
            os.path.join(experiment_folder, "simulated_annealing", "logs", "job_info.csv")
        ),
        "ilp_solver": load_job_info(
            os.path.join(os.path.join(experiment_folder, "ilp_solver", "logs"), "job_info.csv")
        ),
    }

    if dfs["ecmp"] is None or dfs["mcvlc"] is None:
        print("No jobs to analyze")
        print("-" * 50)
        return

    jobs, failed_jobs = filter_jobs(dfs=dfs)
    log_jobs(folder=experiment_folder, jobs=jobs, failed_jobs=failed_jobs, dfs=dfs)
    log_flows(folder=experiment_folder, jobs=jobs)


def log_jobs(folder: str, jobs: dict, failed_jobs: dict, dfs: dict):
    with open(folder + "/successful_jobs.csv", "w+") as f:
        f.write("job_id,epoch\n")
        for job_id in jobs:
            epochs = list(jobs[job_id]["ecmp"].keys())
            for epoch in epochs:
                f.write(f"{job_id},{epoch}\n")

    with open(folder + "/job_comparison.txt", "w+") as f:
        # Header
        f.write(
            "Job ID   ECMP Duration   Greedy Duration  Edge-Coloring Duration  Simulated-Annealing Duration  LP Solver Duration\n"
        )
        write_job_lines(f=f, jobs=jobs, dfs=dfs)
        if len(failed_jobs) > 0:
            f.write("-" * 130 + "\n")
            f.write("Failed Jobs\n")
            f.write(
                "Job ID   ECMP Duration   Greedy Duration  Edge-Coloring Duration  Simulated-Annealing Duration  LP Solver Duration\n"
            )
            write_job_lines(f=f, jobs=jobs, dfs=dfs)


def log_flows(folder: str, jobs: dict):
    dfs = {
        "ecmp": load_flow_info(os.path.join(folder, "ecmp", "logs", "flow_info.csv")),
        "mcvlc": load_flow_info(os.path.join(folder, "mcvlc", "logs", "flow_info.csv")),
        "edge_coloring": load_flow_info(
            os.path.join(folder, "edge_coloring", "logs", "flow_info.csv")
        ),
        "simulated_annealing": load_flow_info(
            os.path.join(folder, "simulated_annealing", "logs", "flow_info.csv")
        ),
        "ilp_solver": load_flow_info(os.path.join(folder, "lp_solver", "logs", "flow_info.csv")),
    }

    flows = {
        key: dfs[key][dfs[key]["job_id"].isin(jobs.keys())].groupby(["job_id", "epoch"])
        for key in dfs
    }

    if len(flows["ecmp"]) == 0 or len(flows["mcvlc"]) == 0:
        print("No flows to analyze")
        print("-" * 50)
        return

    with open(folder + "/flow_comparison.txt", "w+") as f:
        # Header
        write_flows_metric(
            f=f,
            metric="Duration",
            metric_spacing="%-8d %-7d %-15s %-17s %-24s %-30s %-20s\n",
            jobs=jobs,
            flows=flows,
        )
        f.write("-" * 130 + "\n")
        write_flows_metric(
            f=f,
            metric="Throughput",
            metric_spacing="%-8d %-7d %-17s %-19s %-26s %-32s %-20s\n",
            jobs=jobs,
            flows=flows,
        )
        f.write("-" * 130 + "\n")
        f.write("Job ID.  ECMP Flows\n")
        for job_id, flow_ids in jobs.items():
            f.write(f"{job_id}        {flow_ids['ecmp']}\n")

        f.write("-" * 130 + "\n")

        f.write("Job ID.  Greedy Flows\n")
        for job_id, flow_ids in jobs.items():
            f.write(f"{job_id}        {flow_ids['mcvlc']}\n")

        f.write("-" * 130 + "\n")

        f.write("Job ID.  Edge-Coloring Flows\n")
        for job_id, flow_ids in jobs.items():
            f.write(f"{job_id}        {flow_ids['edge_coloring']}\n")
        f.write("-" * 130 + "\n")

        if "simulated_annealing" in dfs:
            f.write("Job ID.  Simulated-Annealing Flows\n")
            for job_id, flow_ids in jobs.items():
                f.write(f"{job_id}        {flow_ids['simulated_annealing']}\n")
            f.write("-" * 130 + "\n")

        f.write("Job ID.  ILP Solver Flows\n")
        for job_id, flow_ids in jobs.items():
            f.write(f"{job_id}        {flow_ids['ilp_solver']}\n")
