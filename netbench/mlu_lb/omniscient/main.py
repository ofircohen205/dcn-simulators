from mlu_lb.omniscient.centralized_controller import CentralizedController
from mlu_lb.omniscient.solver import solve_ilp
import typer

from mlu_lb.omniscient.utils import load_virtual_rings, get_ring_ids, group_virtual_rings
from mlu_lb.schemas.distributed_training import DistributedTraining
from mlu_lb.utils.graph_loader import get_core_ids

app = typer.Typer()


@app.command()
def omniscient_results(
    level: int,
    tors: int,
    bw: int,
    load: float,
    link_failure_rate: float,
    parallel: DistributedTraining,
):
    ring_ids = get_ring_ids(level, tors, bw, load, link_failure_rate, parallel)
    cores = tors // 2
    servers = cores * tors
    controller = CentralizedController(level=level, tors=tors, cores=cores, servers=servers)
    virtual_rings = load_virtual_rings(level, tors, bw, load, parallel, ring_ids)
    grouped_virtual_rings = group_virtual_rings(virtual_rings)
    for group in grouped_virtual_rings:
        controller.cleanup()
        print("-" * 100)
        group_tors = tuple(map(lambda item: set(item["tors"]), group.values()))
        possible_collisions_tors = set()
        for i, tors_a in enumerate(group_tors):
            for j, tors_b in enumerate(group_tors):
                if tors_a != tors_b and tors_a.intersection(tors_b):
                    possible_collisions_tors.add(i)
                    possible_collisions_tors.add(j)

        if len(possible_collisions_tors) < 1:
            print("No possible collisions, skipping...")
            continue

        group_servers = tuple(map(lambda item: tuple(item["servers"]), group.values()))
        controller.to_virtual_links(group_servers)
        model = controller.construct_model()
        try:
            solve_ilp(model=model)
        except ValueError as e:
            raise e

        print(f"ToRs: {group_tors}")
        print(f"Possible collisions: {possible_collisions_tors}")

        mlu = model.objVal
        print(f"Objective (alpha / MLU): {mlu}")

        solution = [
            var.varName for var in model.getVars() if var.varName.startswith("x_p_l") and var.Xn > 0
        ]
        virtual_link_paths = []
        core_ids = sorted(get_core_ids(controller.graph))
        for i, vlink_var_ids in controller.vrings_vlinks_mapping.items():
            # if i not in possible_collisions_tors:
            #     continue
            for j, vlink_var_id in enumerate(vlink_var_ids):
                src_tor, dst_tor = controller.ilp_paths_mapping[vlink_var_id]
                vlink_index = list(controller.ilp_paths_mapping.values()).index((src_tor, dst_tor))
                path_index = int(
                    next(filter(lambda k: f"_{vlink_index}" in k, solution))
                    .replace("]", "")
                    .split("[")[-1]
                )
                src = group_servers[i][j][0]
                dst = group_servers[i][j][1]
                core = core_ids[path_index]
                path = (src, src_tor, core, dst_tor, dst)
                virtual_link_paths.append(path)
                print(path)

        # print(virtual_link_paths)


if __name__ == "__main__":
    app()
