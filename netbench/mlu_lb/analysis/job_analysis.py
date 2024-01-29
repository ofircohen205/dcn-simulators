import os.path as osp
from os import makedirs
from typer import Typer
import pandas as pd

from mlu_lb.analysis.utils import get_jobs, save_cdf, get_base_folder
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
    makedirs(osp.join(plots_folder, "training_iteration_time", "ecmp"), exist_ok=True)
    makedirs(osp.join(plots_folder, "training_iteration_time", "mcvlc"), exist_ok=True)
    makedirs(osp.join(plots_folder, "training_iteration_time", "fast_edge_coloring"), exist_ok=True)
    ecmp_training_iteration_cdf = osp.join(
        plots_folder,
        "training_iteration_time",
        "ecmp",
        f"cdf-bw_{bw}-load_{load}.cdf",
    )
    mcvlc_training_iteration_cdf = osp.join(
        plots_folder,
        "training_iteration_time",
        "mcvlc",
        f"cdf-bw_{bw}-load_{load}.cdf",
    )
    edge_coloring_training_iteration_cdf = osp.join(
        plots_folder,
        "training_iteration_time",
        "fast_edge_coloring",
        f"cdf-bw_{bw}-load_{load}.cdf",
    )

    jobs = get_jobs(n_tors, bw, load, parallel, link_failure_rate)
    ecmp_jobs = []
    mcvlc_jobs = []
    edge_coloring_jobs = []
    for job_id in jobs.keys():
        job_file = f"job_{job_id}-VGG19.csv"
        ecmp_job = pd.read_csv(osp.join(ecmp_folder, job_file))
        ecmp_jobs.append(ecmp_job.apply(lambda x: pd.to_numeric(x, errors="coerce")).dropna())
        mcvlc_job = pd.read_csv(osp.join(mcvlc_folder, job_file))
        mcvlc_jobs.append(mcvlc_job.apply(lambda x: pd.to_numeric(x, errors="coerce")).dropna())
        edge_coloring_job = pd.read_csv(osp.join(edge_coloring_folder, job_file))
        edge_coloring_jobs.append(
            edge_coloring_job.apply(lambda x: pd.to_numeric(x, errors="coerce")).dropna()
        )

    if len(ecmp_jobs) == 0 or len(mcvlc_jobs) == 0 or len(edge_coloring_jobs) == 0:
        print("No flows to analyze")
        print("-" * 50)
        return

    ecmp_jobs_df = pd.concat(ecmp_jobs)
    ecmp_epoch_total_time = ecmp_jobs_df["epoch_total_time"] / 1e9
    save_cdf(ecmp_epoch_total_time, ecmp_training_iteration_cdf)

    mcvlc_jobs_df = pd.concat(mcvlc_jobs)
    mcvlc_epoch_total_time = mcvlc_jobs_df["epoch_total_time"] / 1e9
    save_cdf(mcvlc_epoch_total_time, mcvlc_training_iteration_cdf)

    edge_coloring_jobs_df = pd.concat(edge_coloring_jobs)
    edge_coloring_epoch_total_time = edge_coloring_jobs_df["epoch_total_time"] / 1e9
    save_cdf(edge_coloring_epoch_total_time, edge_coloring_training_iteration_cdf)
    print("-" * 50)


if __name__ == "__main__":
    app()
