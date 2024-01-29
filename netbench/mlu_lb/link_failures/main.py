from typer import Typer
from os import getcwd, listdir
import os.path as osp
import pandas as pd
import random

from mlu_lb.schemas.distributed_training import DistributedTraining

app = Typer()


@app.command()
def generate_failed_links(
    n_tors: int,
    load: float,
    link_failure_rate: float,
    parallel: DistributedTraining,
):
    n_cores = n_tors // 2
    n_servers = n_tors * n_cores

    traffic_pairs_dir = osp.join(
        getcwd(),
        "traffic_pairs",
        "2_level",
        f"{n_tors}_tors",
        f"{n_cores}_cores",
        f"{n_servers}_servers",
        parallel.value,
        "42",
        f"load_{load}",
    )

    active_tors: set = set()
    for job_dir in listdir(traffic_pairs_dir):
        if job_dir.endswith(".txt"):
            continue
        for traffic_pair in listdir(osp.join(traffic_pairs_dir, job_dir)):
            job_df = pd.read_csv(osp.join(traffic_pairs_dir, job_dir, traffic_pair), delimiter=" ")
            src_tors = set(job_df["src_tor"].unique())
            dst_tors = set(job_df["dst_tor"].unique())
            active_tors = active_tors.union(src_tors).union(dst_tors)

    cores = list(range(n_tors, n_tors + n_cores))
    total_links = n_tors * n_cores
    n_failed_links = int(total_links * link_failure_rate)
    cntr = 0
    failed_links: dict = {}
    while cntr < n_failed_links:
        tor = random.choice(list(active_tors))
        if tor not in failed_links.keys():
            failed_links[tor] = []
        core = random.choice(cores)
        if core in failed_links[tor]:  # already failed
            continue

        if failed_links[tor] == cores:  # ToR cannot be isolated
            continue

        failed_links[tor].append(core)
        cntr += 1

    with open(osp.join(traffic_pairs_dir, f"failed_links_{link_failure_rate}.txt"), "w") as f:
        for tor in failed_links.keys():
            for core in failed_links[tor]:
                f.write(f"{tor} {core}\n")


if __name__ == "__main__":
    app()
