import os.path as osp
from os import getcwd, makedirs
from typer import Typer

from mlu_lb.traffic_pairs.hybrid_parallelism import (
    place_hybrid_parallelism_jobs,
    place_data_parallelism_jobs,
)
from mlu_lb.traffic_pairs.writer import (
    write_job_virtual_links_file,
    write_data_parallel_virtual_links_file,
)
from mlu_lb.utils.graph_loader import (
    get_servers_under_tor,
    load_graph_from_file,
)
from mlu_lb.schemas.machines import Machines


app = Typer()

TOPOLOGY_FILENAME_2LEVEL = "fat_tree-{n_tors}_tors-{n_cores}_cores-{n_servers}_servers.topology"
TOPOLOGY_FILENAME_3LEVEL = "fat_tree-k{k}-{n_servers}_servers.topology"


@app.command()
def generate_data_parallel_pairs(
    machine_name: str,
    level: int,
    n_tors: int,
    load: float,
    seed: str,
):
    n_cores = n_tors // 2
    n_servers = n_tors * n_cores
    topology_filename = (
        TOPOLOGY_FILENAME_2LEVEL.format(n_tors=n_tors, n_cores=n_cores, n_servers=n_servers)
        if level == 2
        else TOPOLOGY_FILENAME_3LEVEL.format(k=n_tors, n_servers=n_servers)
    )
    graph = load_graph_from_file(
        topology_fname=osp.join(
            getcwd(), "topologies", "fat_tree", f"{level}_level", topology_filename
        )
    )
    machine = Machines[machine_name].value
    jobs = place_data_parallelism_jobs(
        servers_under_tor=get_servers_under_tor(graph), machine=machine, load=load
    )

    if len(jobs) == 0:
        print("No jobs generated")
        return

    if level == 2:
        experiment_dir = osp.join(
            getcwd(),
            "traffic_pairs",
            f"{level}_level",
            f"{n_tors}_tors",
            f"{n_cores}_cores",
            f"{n_servers}_servers",
            "data_parallel",
            seed,
            f"load_{load}",
        )
    else:
        experiment_dir = osp.join(
            getcwd(),
            "traffic_pairs",
            f"{level}_level",
            f"k{n_tors}",
            f"{n_servers}_servers",
            "data_parallel",
            seed,
            f"load_{load}",
        )
    for job in jobs:
        job_dir = osp.join(experiment_dir, f"job_{job.job_id}-VGG19")
        makedirs(job_dir, exist_ok=True)
        dp_filename = osp.join(job_dir, "data_parallelism.txt")
        write_data_parallel_virtual_links_file(graph=graph, dp_filename=dp_filename, job=job)


@app.command()
def generate_hybrid_parallel_pairs(
    machine_name: str,
    level: int,
    n_tors: int,
    load: float,
    seed: str,
):
    n_cores = n_tors // 2
    n_servers = n_tors * n_cores
    topology_filename = (
        TOPOLOGY_FILENAME_2LEVEL.format(n_tors=n_tors, n_cores=n_cores, n_servers=n_servers)
        if level == 2
        else TOPOLOGY_FILENAME_3LEVEL.format(k=n_tors, n_servers=n_servers)
    )
    graph = load_graph_from_file(
        topology_fname=osp.join(
            getcwd(), "topologies", "fat_tree", f"{level}_level", topology_filename
        )
    )
    machine = Machines[machine_name].value
    jobs = place_hybrid_parallelism_jobs(get_servers_under_tor(graph), machine, load)

    if len(jobs) == 0:
        print("No jobs generated")
        return

    if level == 2:
        experiment_dir = osp.join(
            getcwd(),
            "traffic_pairs",
            f"{level}_level",
            f"{n_tors}_tors",
            f"{n_cores}_cores",
            f"{n_servers}_servers",
            "hybrid_parallel",
            seed,
            f"load_{load}",
        )
    else:
        experiment_dir = osp.join(
            getcwd(),
            "traffic_pairs",
            f"{level}_level",
            f"k{n_tors}",
            f"{n_servers}_servers",
            "hybrid_parallel",
            seed,
            f"load_{load}",
        )
    for job in jobs:
        job_dir = osp.join(experiment_dir, f"job_{job.job_id}-{job.model.name}")
        makedirs(job_dir, exist_ok=True)
        pp_filename = osp.join(job_dir, "pipeline.txt")
        dp_filename = osp.join(job_dir, "data_parallelism.txt")
        write_job_virtual_links_file(
            graph=graph, pp_filename=pp_filename, dp_filename=dp_filename, job=job
        )


if __name__ == "__main__":
    app()
