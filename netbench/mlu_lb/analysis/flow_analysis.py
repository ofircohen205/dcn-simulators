import os.path as osp
from os import makedirs
from typer import Typer
import pandas as pd

from mlu_lb.analysis.utils import (
    get_jobs,
    save_cdf,
    get_base_folder,
    analyze_flows,
)
from mlu_lb.schemas.distributed_training import DistributedTraining

app = Typer()

OUTPUT_FOLDER = osp.join(
    "experiments",
    "plots",
)

bandwidths = [10, 25, 100, 200, 400]
loads = [0.1, 0.25, 0.5, 0.75, 0.9, 1.0]
link_failure_rates = [0.0, 0.01, 0.05, 0.1]


@app.command()
def compare_runtime(n_tors: int, parallel: DistributedTraining):
    for link_failure_rate in link_failure_rates:
        for bw in bandwidths:
            for load in loads:
                compare_runtime_helper(n_tors, bw, load, parallel, link_failure_rate)


def compare_runtime_helper(
    n_tors: int, bw: int, load: float, parallel: DistributedTraining, link_failure_rate: float
):
    base_folder = get_base_folder(n_tors, bw, load, parallel, link_failure_rate)
    ecmp_folder = osp.join(base_folder, "ecmp")
    mcvlc_folder = osp.join(base_folder, "mcvlc")
    edge_coloring_folder = osp.join(base_folder, "fast_edge_coloring")
    if (
        not osp.exists(ecmp_folder)
        or not osp.exists(mcvlc_folder)
        or not osp.exists(edge_coloring_folder)
    ):
        return
    params = f"bw={bw}, load={load}, link_failure_rate={link_failure_rate}"
    print(f"Analysis of flows for ECMP vs MCVLC vs Fast-Edge-Coloring ({params}):")
    plots_folder = osp.join(
        OUTPUT_FOLDER,
        f"{n_tors}_tors",
        parallel.value,
        f"link_failure_rate_{link_failure_rate}",
    )
    makedirs(osp.join(plots_folder, "fct", "ecmp"), exist_ok=True)
    makedirs(osp.join(plots_folder, "fct", "mcvlc"), exist_ok=True)
    makedirs(osp.join(plots_folder, "fct", "fast_edge_coloring"), exist_ok=True)
    makedirs(osp.join(plots_folder, "throughput", "ecmp"), exist_ok=True)
    makedirs(osp.join(plots_folder, "throughput", "mcvlc"), exist_ok=True)
    makedirs(osp.join(plots_folder, "throughput", "fast_edge_coloring"), exist_ok=True)
    ecmp_fct_cdf = osp.join(
        plots_folder,
        "fct",
        "ecmp",
        f"cdf-bw_{bw}-load_{load}.cdf",
    )
    ecmp_throughput_cdf = osp.join(
        plots_folder,
        "throughput",
        "ecmp",
        f"cdf-bw_{bw}-load_{load}.cdf",
    )
    mcvlc_fct_cdf = osp.join(
        plots_folder,
        "fct",
        "mcvlc",
        f"cdf-bw_{bw}-load_{load}.cdf",
    )
    mcvlc_throughput_cdf = osp.join(
        plots_folder,
        "throughput",
        "mcvlc",
        f"cdf-bw_{bw}-load_{load}.cdf",
    )
    edge_coloring_fct_cdf = osp.join(
        plots_folder,
        "fct",
        "fast_edge_coloring",
        f"cdf-bw_{bw}-load_{load}.cdf",
    )
    edge_coloring_throughput_cdf = osp.join(
        plots_folder,
        "throughput",
        "fast_edge_coloring",
        f"cdf-bw_{bw}-load_{load}.cdf",
    )

    jobs = get_jobs(n_tors, bw, load, parallel, link_failure_rate)
    ecmp_flows = []
    mcvlc_flows = []
    edge_coloring_flows = []
    for job_id in jobs.keys():
        ecmp_job_flows, edge_coloring_job_flows, mcvlc_job_flows = analyze_flows(
            ecmp_folder, edge_coloring_folder, mcvlc_folder, link_failure_rate, job_id
        )
        ecmp_flows.append(ecmp_job_flows)
        mcvlc_flows.append(mcvlc_job_flows)
        edge_coloring_flows.append(edge_coloring_job_flows)

    if len(ecmp_flows) == 0 or len(mcvlc_flows) == 0 or len(edge_coloring_flows) == 0:
        print("No flows to analyze")
        print("-" * 50)
        return

    ecmp_flows_df = pd.concat(ecmp_flows)
    ecmp_fct = ecmp_flows_df["flow_total_time"] / 1e9
    ecmp_throughput = 8 * ecmp_flows_df["total_bytes_received"] / ecmp_flows_df["flow_total_time"]
    save_cdf(ecmp_fct, ecmp_fct_cdf)
    save_cdf(ecmp_throughput, ecmp_throughput_cdf)

    mcvlc_flows_df = pd.concat(mcvlc_flows)
    mcvlc_fct = mcvlc_flows_df["flow_total_time"] / 1e9
    mcvlc_throughput = (
        8 * mcvlc_flows_df["total_bytes_received"] / mcvlc_flows_df["flow_total_time"]
    )
    save_cdf(mcvlc_fct, mcvlc_fct_cdf)
    save_cdf(mcvlc_throughput, mcvlc_throughput_cdf)

    edge_coloring_flows_df = pd.concat(edge_coloring_flows)
    edge_coloring_fct = edge_coloring_flows_df["flow_total_time"] / 1e9
    edge_coloring_throughput = (
        8
        * edge_coloring_flows_df["total_bytes_received"]
        / edge_coloring_flows_df["flow_total_time"]
    )
    save_cdf(edge_coloring_fct, edge_coloring_fct_cdf)
    save_cdf(edge_coloring_throughput, edge_coloring_throughput_cdf)
    print("-" * 50)


if __name__ == "__main__":
    app()
