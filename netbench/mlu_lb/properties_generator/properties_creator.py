import os.path as osp
from os import makedirs

from mlu_lb.schemas.distributed_training import DistributedTraining
from mlu_lb.schemas.routing import Routing


def create_tmp_properties_file(
    bandwidth: int,
    distributed_training: DistributedTraining,
    tors: int,
    cores: int,
    servers: int,
    level: int,
    load: float,
    link_failure_rate: float,
    routing: Routing,
) -> str:
    seed = 43 if link_failure_rate > 0 else 42
    topology_fname = osp.join(
        "topologies",
        "fat_tree",
        f"{level}_level",
        f"fat_tree-{tors}_tors-{cores}_cores-{servers}_servers.topology",
    )

    tmp_properties_dir = osp.join(
        "properties",
        distributed_training.value,
        f"{tors}_tors",
        f"bw_{bandwidth}",
        f"load_{load}",
        f"link_failure_rate_{link_failure_rate}",
    )
    makedirs(tmp_properties_dir, exist_ok=True)
    if level == 2:
        base_properties_fname = osp.join(
            "properties",
            f"fat_tree-{tors}_tors-{cores}_cores-{servers}_servers.properties",
        )
        tmp_properties_fname = osp.join(
            tmp_properties_dir,
            f"{routing.value}-fat_tree-{tors}_tors-{cores}_cores-{servers}_servers.properties",
        )
    else:
        base_properties_fname = osp.join(
            "properties",
            f"fat_tree-k{tors}-{servers}_servers.properties",
        )
        tmp_properties_fname = osp.join(
            tmp_properties_dir,
            f"fat_tree-k{tors}-{servers}_servers-bw_{bandwidth}-load_{load}-{seed}.properties",
        )

    if osp.exists(tmp_properties_fname):
        return tmp_properties_fname

    with open(base_properties_fname, "r") as f:
        lines = f.readlines()
        for i, line in enumerate(lines):
            if line.startswith("scenario_topology_file="):
                lines[i] = f"scenario_topology_file={topology_fname}\n"
            if line.startswith("seed="):
                lines[i] = f"seed={seed}\n"
            if line.startswith("run_folder_base_dir="):
                run_folder_base_dir = osp.join(
                    "experiments",
                    distributed_training.value,
                    f"{level}_level",
                    f"{tors}_tors",
                    f"bw_{bandwidth}",
                    f"load_{load}",
                    f"link_failure_rate_{link_failure_rate}",
                )
                lines[i] = f"run_folder_base_dir={run_folder_base_dir}\n"
            if line.startswith("run_folder_name="):
                lines[i] = f"run_folder_name={routing.value}\n"
            if line.startswith("routing_scheme="):
                lines[i] = f"routing_scheme={routing.value}\n"
            if line.startswith("base_traffic_pairs_dir="):
                base_traffic_pairs_dir = osp.join(
                    "traffic_pairs",
                    f"{level}_level",
                    f"{tors}_tors",
                    f"{cores}_cores",
                    f"{servers}_servers",
                    distributed_training.value,
                    "42",
                    f"load_{load}",
                )
                lines[i] = f"base_traffic_pairs_dir={base_traffic_pairs_dir}\n"
            if line.startswith("link_bandwidth_bit_per_ns="):
                lines[i] = f"link_bandwidth_bit_per_ns={bandwidth}\n"
            if line.startswith("traffic_pair_type="):
                lines[i] = f"traffic_pair_type={distributed_training.value}\n"
            if line.startswith("allow_link_failures="):
                lines[i] = f"allow_link_failures={str(seed == 43).lower()}\n"
            if line.startswith("link_failure_rate="):
                lines[i] = f"link_failure_rate={link_failure_rate}\n"

    with open(tmp_properties_fname, "w") as f:
        f.writelines(lines)

    return tmp_properties_fname
