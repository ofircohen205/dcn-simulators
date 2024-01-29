import os
from os import getcwd, makedirs
from typer import Typer

from external.jobs_generator.job_generator import create_jobs
from external.jobs_generator.writer import write_ddp_file
from external.schemas.accelerators import Accelerators
from external.utils.graph import get_tor_to_hosts

app = Typer()


@app.command()
def gen_ddp_pairs(
    accelerator_name: str,
    n_tors: int,
    num_concurrent_jobs: int,
    data_parallelism_dim: int,
):
    radix = n_tors // 2
    accelerator = Accelerators[accelerator_name].value
    tor_to_nics = get_tor_to_hosts(n_tors=n_tors)
    jobs = create_jobs(
        tor_to_nics=tor_to_nics,
        accelerator=accelerator,
        radix=radix,
        num_concurrent_jobs=num_concurrent_jobs,
        data_parallelism_dim=data_parallelism_dim,
    )

    if len(jobs) == 0:
        print("No jobs generated")
        return

    traffic_pairs_dir = os.path.join(
        getcwd(),
        "traffic_pairs",
        f"concurrent_jobs_{num_concurrent_jobs}",
        f"ring_size_{data_parallelism_dim}",
    )
    makedirs(traffic_pairs_dir, exist_ok=True)

    for job in jobs:
        ddp_filename = os.path.join(traffic_pairs_dir, f"job_{job.job_id}-{job.model.name}.txt")
        write_ddp_file(n_tors=n_tors, filename=ddp_filename, job=job)


if __name__ == "__main__":
    app()
