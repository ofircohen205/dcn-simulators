import os
import pickle

from external.ipc.pool import SharedMemoryPool
from external.omniscient.controller import CentralizedController
from external.utils.ipc import fetch_commodities
from typer import Typer

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


if __name__ == "__main__":
    app()
