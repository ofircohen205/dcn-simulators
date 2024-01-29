from os import makedirs
import os.path as osp
from typing import List, Tuple


def write_file(
    topology_file: str,
    total_nodes: int,
    total_links: int,
    min_tor_id: int,
    max_tor_id: int,
    min_core_id: int,
    max_core_id: int,
    min_server_id: int,
    max_server_id: int,
    uplinks: List[Tuple[int, int]],
    downlinks: List[Tuple[int, int]],
    min_aggregation_id: int = -1,
    max_aggregation_id: int = -1,
):
    makedirs(osp.dirname(topology_file), exist_ok=True)
    with open(topology_file, "w") as f:
        f.write(f"|V|={total_nodes}\n")
        f.write(f"|E|={total_links}\n")

        f.write(f"ToRs=incl_range({min_tor_id},{max_tor_id})\n")
        if min_aggregation_id != -1 and max_aggregation_id != -1:
            f.write(f"Aggregation=incl_range({min_aggregation_id},{max_aggregation_id})\n")
        f.write(f"Cores=incl_range({min_core_id},{max_core_id})\n")
        if min_server_id != -1 and max_server_id != -1:
            f.write(f"Servers=incl_range({min_server_id},{max_server_id})\n\n")

        f.write("# Links:\n")
        f.write("# Uplinks:\n")
        for link in uplinks:
            f.write(f"{link[0]} {link[1]}\n")
        f.write("# Downlinks:\n")
        for link in downlinks:
            f.write(f"{link[0]} {link[1]}\n")
        return topology_file
