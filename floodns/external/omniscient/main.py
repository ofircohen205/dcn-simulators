import os
import pickle
from itertools import combinations
from os import getcwd

from typer import Typer

from external.ipc.pool import SharedMemoryPool
from external.omniscient.controller import CentralizedController
from external.schemas.distributed_training import DistributedTraining
from external.schemas.omniscient import OmniscientSolution
from external.utils.graph import get_tor_of_host
from external.utils.ipc import fetch_commodities
from external.utils.omniscient import get_job_ids, load_jobs, group_jobs, fetch_failed_links

app = Typer()


@app.command()
def omniscient(run_dir: str):
    shared_memory = SharedMemoryPool.get(seed=42, run_dir=run_dir)
    response = shared_memory.read()
    shared_memory.close()
    commodities = fetch_commodities(response["src_dst_pairs"])
    failed_links = response.get("failed_links", [])
    if isinstance(failed_links, str):
        failed_links = set(eval(failed_links))
    failed_cores = response.get("failed_cores", [])
    if isinstance(failed_cores, str):
        failed_cores = set(
            map(
                int, failed_cores.replace("[", "").replace("]", "").replace("Node#", "").split(", ")
            )
        )
    n_tors = int(response["num_tors"])
    if not os.path.exists(os.path.join(run_dir, "controller.pkl")):
        controller = CentralizedController(n_tors=n_tors)
        with open(os.path.join(run_dir, "controller.pkl"), "wb") as f:
            pickle.dump(controller, f)
    else:
        with open(os.path.join(run_dir, "controller.pkl"), "rb") as f:
            controller = pickle.load(f)
    controller.cleanup()
    controller.failed_links = failed_links
    controller.failed_cores = failed_cores
    controller.to_virtual_links(commodities=commodities)
    controller.construct_model()
    controller.solve()
    controller.problem.writeMPS(os.path.join(run_dir, "model.mps"))

    assignments = controller.fetch_solution(commodities=commodities)
    shared_memory.write(assignments)


@app.command()
def omniscient_no_failures(
    level: int,
    n_tors: int,
    bw: int,
    load: float,
    parallel: DistributedTraining,
):
    jobs_dir = os.path.join(
        getcwd(),
        "traffic_pairs",
        f"{level}_level",
        f"{n_tors}_tors",
        parallel.value,
        f"load_{load}",
    )
    job_ids = get_job_ids(jobs_dir=jobs_dir)
    jobs = load_jobs(
        level=level, n_tors=n_tors, bw=bw, load=load, parallel=parallel, job_ids=job_ids
    )
    omniscient_results(n_tors=n_tors, jobs=jobs, failed_links=set())


@app.command()
def omniscient_node_failures(
    level: int,
    n_tors: int,
    bw: int,
    load: float,
    num_failed_nodes: int,
    parallel: DistributedTraining,
):
    jobs_dir = os.path.join(
        getcwd(),
        "traffic_pairs",
        f"{level}_level",
        f"{n_tors}_tors",
        parallel.value,
        f"load_{load}",
    )
    job_ids = get_job_ids(jobs_dir=jobs_dir)
    jobs = load_jobs(
        level=level, n_tors=n_tors, bw=bw, load=load, parallel=parallel, job_ids=job_ids
    )
    failure_file = os.path.join(
        getcwd(),
        "traffic_pairs",
        f"{level}_level",
        f"{n_tors}_tors",
        parallel.value,
        f"{num_failed_nodes}_failed_nodes.txt",
    )
    failed_links = fetch_failed_links(failure_file=failure_file)
    omniscient_results(n_tors=n_tors, jobs=jobs, failed_links=failed_links)


@app.command()
def omniscient_link_failures(
    level: int,
    n_tors: int,
    bw: int,
    load: float,
    link_failure_rate: float,
    parallel: DistributedTraining,
):
    jobs_dir = os.path.join(
        getcwd(),
        "traffic_pairs",
        f"{level}_level",
        f"{n_tors}_tors",
        parallel.value,
        f"load_{load}",
    )
    job_ids = get_job_ids(jobs_dir=jobs_dir)
    jobs = load_jobs(
        level=level, n_tors=n_tors, bw=bw, load=load, parallel=parallel, job_ids=job_ids
    )
    failure_file = os.path.join(
        getcwd(),
        "traffic_pairs",
        f"{level}_level",
        f"{n_tors}_tors",
        parallel.value,
        f"{link_failure_rate}_failed_links.txt",
    )
    failed_links = fetch_failed_links(failure_file=failure_file)
    omniscient_results(n_tors=n_tors, jobs=jobs, failed_links=failed_links)


@app.command()
def omniscient_test(n_tors: int):
    min_server_id = n_tors + n_tors // 2
    max_server_id = min_server_id + n_tors * (n_tors // 2) - 1
    src_dst_pairs = {
        0: (min_server_id, min_server_id + 2),  # (1,3)
        1: (min_server_id, min_server_id + 4),  # (1,5)
        2: (min_server_id + 1, max_server_id),  # (2,8)
        3: (min_server_id + 1, max_server_id - 2),  # (2,6)
        4: (min_server_id + 2, min_server_id),  # (3,1)
        5: (min_server_id + 2, max_server_id - 3),  # (3,5)
        6: (max_server_id - 2, max_server_id),  # (6,8)
        7: (max_server_id - 3, min_server_id),  # (5,1)
        8: (max_server_id - 2, min_server_id + 3),  # (6,4)
        9: (max_server_id - 2, max_server_id),  # (6,8)
        10: (max_server_id - 1, min_server_id),  # (7,1)
        11: (max_server_id - 1, min_server_id + 2),  # (7,3)
        12: (max_server_id, max_server_id - 2),  # (8,6)
    }
    controller = CentralizedController(n_tors=n_tors)
    # controller.failed_links = {(0, 4), (1, 4), (2, 4), (3, 4), (4, 0), (4, 1), (4, 2), (4, 3)}
    controller.to_virtual_links(src_dst_pairs=src_dst_pairs)
    controller.construct_model()
    obj_val = controller.solve()
    print(f"Objective (alpha / MLU): {obj_val}")

    solution = controller.fetch_solution(src_dst_pairs=src_dst_pairs)
    for src in solution:
        src_tor = get_tor_of_host(tor_to_hosts=controller.tor_to_hosts, host=src)
        for dst in solution[src]:
            dst_tor = get_tor_of_host(tor_to_hosts=controller.tor_to_hosts, host=dst)
            for idx in solution[src][dst]:
                core = controller.cores[solution[src][dst][idx]]
                print(f"({src},{src_tor},{core},{dst_tor},{dst})")


def omniscient_results(n_tors: int, jobs: list, failed_links: set):
    controller = CentralizedController(n_tors=n_tors)
    grouped_jobs = group_jobs(jobs=jobs)
    for group in grouped_jobs:
        controller.cleanup()

        job_ids = group.keys()
        jobs_solutions = {
            job_id: OmniscientSolution(
                job_id=job_id,
                job_colliding_with=set(),
                virtual_links=set(),
                tors=group[job_id]["tors"],
                common_tors=set(),
                paths=set(),
            )
            for job_id in job_ids
        }
        pair_job_ids = list(combinations(job_ids, 2))
        possible_collisions = [
            (
                job_id_a,
                job_id_b,
                group[job_id_a]["tors"].intersection(group[job_id_b]["tors"]),
            )
            for job_id_a, job_id_b in pair_job_ids
            if group[job_id_a]["tors"].intersection(group[job_id_b]["tors"])
        ]

        for possible_collision in possible_collisions:
            job_id_a, job_id_b, common_tors = possible_collision
            jobs_solutions[job_id_a].job_colliding_with.add(job_id_b)
            jobs_solutions[job_id_b].job_colliding_with.add(job_id_a)
            jobs_solutions[job_id_a].common_tors.update(common_tors)
            jobs_solutions[job_id_b].common_tors.update(common_tors)

        if len(possible_collisions) < 1:
            print("No possible collisions, skipping...")
            continue

        src_dst_pairs = {}
        idx = 0
        for job_id, job in group.items():
            for virtual_link in job["virtual_links"]:
                src_dst_pairs[idx] = tuple(virtual_link)
                jobs_solutions[job_id].virtual_links.add(idx)
                idx += 1
        controller.to_virtual_links(src_dst_pairs=src_dst_pairs)
        controller.failed_links = failed_links
        controller.construct_model()
        obj_val = controller.solve()
        print(f"Objective (alpha / MLU): {obj_val}")

        solution = controller.fetch_solution(src_dst_pairs=src_dst_pairs)
        for src in solution:
            src_tor = get_tor_of_host(tor_to_hosts=controller.tor_to_hosts, host=src)
            for dst in solution[src]:
                dst_tor = get_tor_of_host(tor_to_hosts=controller.tor_to_hosts, host=dst)
                for idx in solution[src][dst]:
                    core = controller.cores[solution[src][dst][idx]]
                    for job_solution in jobs_solutions.values():
                        if idx in job_solution.virtual_links:
                            job_solution.paths.add((src, src_tor, core, dst_tor, dst))

        for job_solution in jobs_solutions.values():
            print("-" * 50)
            print(f"Job {job_solution.job_id}")
            print(f"Colliding with: {job_solution.job_colliding_with}")
            print(f"Common ToRs: {job_solution.common_tors}")
            print(f"Paths:")
            for path in job_solution.paths:
                print(f"({path[0]},{path[-1]}): {path}")

        print("-" * 100)


if __name__ == "__main__":
    app()
