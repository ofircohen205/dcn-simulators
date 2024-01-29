import os
import typer
from os import getcwd

from external.cdf_generation.utils import (
    load_job_ids,
    save_fct_cdf,
    load_connection_info_df,
    save_throughput_cdf,
)
from external.schemas.routing import Routing

app = typer.Typer()

NUM_FAILED_CORES = [0, 1, 4, 8]
NUM_CONCURRENT_JOBS = [1, 2, 3, 4, 5]
RING_SIZES = [2, 4, 8]
BASE_PATH = os.path.join(getcwd(), "runs")


@app.command()
def generate_flow_completion_time_cdf(routing: Routing):
    for num_jobs in NUM_CONCURRENT_JOBS:
        for num_cores in NUM_FAILED_CORES:
            for ring_size in RING_SIZES:
                if num_jobs in {4, 5} and ring_size == 8:
                    print(
                        f"Skipping concurrent_jobs_{num_jobs} with ring_size_{ring_size} for {routing.value}"
                    )
                    continue
                handle_fct(
                    routing=routing,
                    num_jobs=num_jobs,
                    num_cores=num_cores,
                    ring_size=ring_size,
                )


def handle_fct(num_jobs: int, num_cores: int, routing: Routing, ring_size: int = -1):
    concurrent_jobs_path = os.path.join(
        BASE_PATH,
        f"concurrent_jobs_{num_jobs}",
        f"{num_cores}_core_failures",
        f"ring_size_{ring_size}",
        routing.value,
        "logs_floodns",
    )
    filename = os.path.join(concurrent_jobs_path, "connection_info.csv")
    if not os.path.exists(filename):
        print(f"File {filename} does not exist")
        return

    job_ids_folder = os.path.join(
        BASE_PATH,
        f"concurrent_jobs_{num_jobs}",
        f"{num_cores}_core_failures",
        f"ring_size_{ring_size}",
    )
    jobs = load_job_ids(
        job_ids_folder=job_ids_folder,
        num_concurrent_jobs=num_jobs,
        num_core_failures=num_cores,
        ring_size=ring_size,
    )

    for job_id in jobs["job_id"].unique().tolist():
        epochs = jobs[jobs["job_id"] == job_id]["epoch"].unique().tolist()
        df = load_connection_info_df(filename=filename)
        if df.empty:
            print(f"File {filename} is empty")
            return
        df = df[df["job_id"] == job_id & df["epoch"].isin(epochs)]
        if df.empty:
            print(f"File {filename} is empty")
            return
        data = {
            "epoch": [],
            "duration": [],
            "sent": [],
        }
        for epoch in epochs:
            data["epoch"].append(epoch)

            slowest_stage = df[df["epoch"] == epoch]["duration"].max()
            data["duration"].append(slowest_stage)

            sent_bytes = df[df["epoch"] == epoch]["sent"].max()
            data["sent"].append(sent_bytes)
        save_fct_cdf(
            df=df,
            routing=routing,
            num_jobs=num_jobs,
            num_cores=num_cores,
            ring_size=ring_size,
            job_id=job_id,
        )
        save_throughput_cdf(
            df=df,
            routing=routing,
            num_jobs=num_jobs,
            num_cores=num_cores,
            ring_size=ring_size,
            job_id=job_id,
        )


if __name__ == "__main__":
    app()
