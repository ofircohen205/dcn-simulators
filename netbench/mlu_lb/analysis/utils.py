from os import getcwd, listdir
import os.path as osp
import pandas as pd
import numpy as np
from mlu_lb.schemas.distributed_training import DistributedTraining

# CDF interpolation x-values
plot_x_vals: list = []
plot_x_vals.extend(np.arange(1e-8, 1e-7, 1e-10))
plot_x_vals.extend(np.arange(1e-7, 1e-6, 1e-9))
plot_x_vals.extend(np.arange(1e-6, 1e-5, 1e-8))
plot_x_vals.extend(np.arange(1e-5, 1e-4, 1e-7))
plot_x_vals.extend(np.arange(1e-4, 1e-3, 1e-6))
plot_x_vals.extend(np.arange(1e-3, 1e-2, 1e-5))
plot_x_vals.extend(np.arange(1e-2, 1e-1, 1e-4))
plot_x_vals.extend(np.arange(1e-1, 1e0, 1e-3))
plot_x_vals.extend(np.arange(1e0, 1e1, 1e-2))
plot_x_vals.extend(np.arange(1e1, 1e2, 1e-1))
plot_x_vals.extend(np.arange(1e2, 1e3, 1e0))
plot_x_vals.extend(np.arange(1e3, 1e4, 1e1))
plot_x_vals.extend(np.arange(1e4, 1e5, 1e2))
plot_x_vals.extend(np.arange(1e5, 1e6, 1e3))
plot_x_vals.extend(np.arange(1e6, 1e7, 1e4))
plot_x_vals.extend(np.arange(1e7, 1e8, 1e5))
plot_x_vals.extend(np.arange(1e8, 1e9, 1e6))
plot_x_vals.extend(np.arange(1e9, 1e10, 1e7))
plot_x_vals.extend(np.arange(1e10, 1e11, 1e8))
plot_x_vals.extend(np.arange(1e11, 1e12, 1e9))
plot_x_vals.extend(np.arange(1e12, 1e13, 1e10))


def get_base_folder(
    n_tors: int, bw: int, load: float, parallel: DistributedTraining, link_failure_rate: float
):
    return osp.join(
        getcwd(),
        "experiments",
        parallel.value,
        "2_level",
        f"{n_tors}_tors",
        f"bw_{bw}",
        f"load_{load}",
        f"link_failure_rate_{link_failure_rate}",
    )


def get_jobs(
    n_tors: int, bw: int, load: float, parallel: DistributedTraining, link_failure_rate: float
):
    base_folder = get_base_folder(n_tors, bw, load, parallel, link_failure_rate)
    ecmp_folder = osp.join(base_folder, "ecmp")
    mcvlc_folder = osp.join(base_folder, "mcvlc")
    edge_coloring_folder = osp.join(base_folder, "fast_edge_coloring")

    all_jobs = [
        filename
        for filename in listdir(ecmp_folder)
        if filename.startswith("job_") and filename.endswith(".csv")
    ]
    jobs = {}
    for job in all_jobs:
        job_id = int(job.split("-")[0].split("_")[-1])
        ecmp_average_epoch_time = get_average_epoch_time(df=pd.read_csv(osp.join(ecmp_folder, job)))
        mcvlc_average_epoch_time = get_average_epoch_time(
            df=pd.read_csv(osp.join(mcvlc_folder, job)),
        )
        edge_coloring_average_epoch_time = get_average_epoch_time(
            df=pd.read_csv(osp.join(edge_coloring_folder, job))
        )
        if edge_coloring_average_epoch_time <= ecmp_average_epoch_time:
            jobs[job_id] = (
                edge_coloring_average_epoch_time,
                mcvlc_average_epoch_time,
                ecmp_average_epoch_time,
            )

    return jobs


def analyze_flows(
    ecmp_folder: str,
    edge_coloring_folder: str,
    mcvlc_folder: str,
    link_failure_rate: float,
    job_id: int,
):
    seed = 42 if link_failure_rate == 0 else 43
    ecmp_flows = pd.read_csv(osp.join(ecmp_folder, f"flow_completion-seed_{seed}.csv"))
    mcvlc_flows = pd.read_csv(osp.join(mcvlc_folder, f"flow_completion-seed_{seed}.csv"))
    ecmp_flows = ecmp_flows[ecmp_flows["job_id"] == job_id].reset_index(drop=True)
    mcvlc_flows = mcvlc_flows[mcvlc_flows["job_id"] == job_id].reset_index(drop=True)
    edge_coloring_flows = df = pd.read_csv(
        osp.join(edge_coloring_folder, f"flow_completion-seed_{seed}.csv")
    )
    edge_coloring_flows = edge_coloring_flows[edge_coloring_flows["job_id"] == job_id].reset_index(
        drop=True
    )

    ecmp_series = []
    mcvlc_series = []
    edge_coloring_series = []
    for row in range(min(len(ecmp_flows), len(mcvlc_flows), len(edge_coloring_flows))):
        ecmp_flow = ecmp_flows.iloc[row]
        mcvlc_flow = mcvlc_flows.iloc[row]
        edge_coloring_flow = edge_coloring_flows.iloc[row]
        ecmp_fct = round(ecmp_flow["flow_total_time"] / 1e9, 2)
        edge_coloring_fct = round(edge_coloring_flow["flow_total_time"] / 1e9, 2)
        if (
            edge_coloring_fct <= ecmp_fct
            and ecmp_flow["flow_completed"]
            and edge_coloring_flow["flow_completed"]
        ):
            ecmp_series.append(ecmp_flow)
            mcvlc_series.append(mcvlc_flow)
            edge_coloring_series.append(edge_coloring_flow)

    ecmp_flows = pd.DataFrame(ecmp_series)
    mcvlc_flows = pd.DataFrame(mcvlc_series)
    edge_coloring_flows = pd.DataFrame(edge_coloring_series)
    return ecmp_flows, edge_coloring_flows, mcvlc_flows


def save_cdf(data: pd.Series, cdf_file: str):
    sorted_fct = np.sort(data)
    cdf_y_vals = np.arange(len(sorted_fct)) / float(len(sorted_fct) - 1)
    plot_y_vals = np.interp(plot_x_vals, sorted_fct, cdf_y_vals)
    with open(cdf_file, "w+") as outfile:
        for k in range(0, len(plot_x_vals)):
            if k == len(plot_x_vals) - 1 or plot_y_vals[k + 1] != 0:
                outfile.write(str(plot_x_vals[k]) + "\t" + str(plot_y_vals[k]) + "\n")
            if plot_y_vals[k] == 1 and plot_y_vals[k + 1] == 1:
                break


def get_average_epoch_time(df: pd.DataFrame):
    df = df.apply(lambda x: pd.to_numeric(x, errors="coerce")).dropna()
    average_epoch_time = df["epoch_total_time"].mean() / 1e9
    return average_epoch_time
