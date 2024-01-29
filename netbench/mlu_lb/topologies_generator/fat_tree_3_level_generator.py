from os import getcwd
import os.path as osp

from mlu_lb.topologies_generator.links_generator import (
    build_tor_aggregation_links,
    build_aggregation_core_links,
    build_server_tor_links,
)
from mlu_lb.topologies_generator.topology_writer import write_file


def create_3level(k: int):
    assert k % 2 == 0, "k must be even"
    num_tors = 2 * (k // 2) ** 2  # k^2 / 2
    num_aggregations = 2 * (k // 2) ** 2  # k^2 / 2
    num_cores = (k // 2) ** 2  # k^2 / 4
    num_pods = k
    topology_file = osp.join(
        getcwd(),
        "topologies",
        "fat_tree",
        "3_level",
        f"fat_tree-k{k}.topology",
    )

    total_switches = num_cores + num_tors + num_aggregations
    total_nodes = total_switches

    min_tor_id, max_tor_id = 0, num_tors - 1
    min_aggregation_id, max_aggregation_id = num_tors, num_tors + num_aggregations - 1
    (tor_aggregation_uplinks, tor_aggregation_downlinks,) = build_tor_aggregation_links(
        min_tor_id=min_tor_id,
        max_tor_id=max_tor_id,
        min_aggregation_id=min_aggregation_id,
        max_aggregation_id=max_aggregation_id,
        num_pods=num_pods,
    )

    min_core_id, max_core_id = num_tors + num_aggregations, total_switches - 1
    aggregation_core_uplinks, aggregation_core_downlinks = build_aggregation_core_links(
        min_aggregation_id=min_aggregation_id,
        max_aggregation_id=max_aggregation_id,
        min_core_id=min_core_id,
        max_core_id=max_core_id,
        num_pods=num_pods,
    )

    uplinks = tor_aggregation_uplinks + aggregation_core_uplinks
    downlinks = tor_aggregation_downlinks + aggregation_core_downlinks
    write_file(
        topology_file=topology_file,
        total_nodes=total_nodes,
        total_links=len(uplinks + downlinks),
        min_tor_id=min_tor_id,
        max_tor_id=max_tor_id,
        min_aggregation_id=min_aggregation_id,
        max_aggregation_id=max_aggregation_id,
        min_core_id=min_core_id,
        max_core_id=max_core_id,
        min_server_id=-1,
        max_server_id=-1,
        uplinks=uplinks,
        downlinks=downlinks,
    )


def create_3level_with_server(k: int):
    assert k % 2 == 0, "k must be even"
    num_tors = 2 * (k // 2) ** 2
    num_aggregations = 2 * (k // 2) ** 2
    num_cores = (k // 2) ** 2
    num_servers = 2 * (k // 2) ** 3
    num_pods = k
    topology_file = osp.join(
        getcwd(),
        "topologies",
        "fat_tree",
        "3_level",
        f"fat_tree-k{k}-{num_servers}_servers.topology",
    )

    total_switches = num_cores + num_tors + num_aggregations
    total_nodes = total_switches + num_servers

    min_tor_id, max_tor_id = 0, num_tors - 1
    min_aggregation_id, max_aggregation_id = num_tors, num_tors + num_aggregations - 1
    (tor_aggregation_uplinks, tor_aggregation_downlinks,) = build_tor_aggregation_links(
        min_tor_id=min_tor_id,
        max_tor_id=max_tor_id,
        min_aggregation_id=min_aggregation_id,
        max_aggregation_id=max_aggregation_id,
        num_pods=num_pods,
    )

    min_core_id, max_core_id = num_tors + num_aggregations, total_switches - 1
    aggregation_core_uplinks, aggregation_core_downlinks = build_aggregation_core_links(
        min_aggregation_id=min_aggregation_id,
        max_aggregation_id=max_aggregation_id,
        min_core_id=min_core_id,
        max_core_id=max_core_id,
        num_pods=num_pods,
    )

    min_server_id, max_server_id = total_switches, total_nodes - 1
    server_tor_uplinks, server_tor_downlinks = build_server_tor_links(
        min_tor_id=min_tor_id,
        max_tor_id=max_tor_id,
        min_server_id=min_server_id,
        max_server_id=max_server_id,
    )

    uplinks = server_tor_uplinks + tor_aggregation_uplinks + aggregation_core_uplinks
    downlinks = server_tor_downlinks + tor_aggregation_downlinks + aggregation_core_downlinks
    write_file(
        topology_file=topology_file,
        total_nodes=total_nodes,
        total_links=len(uplinks + downlinks),
        min_tor_id=min_tor_id,
        max_tor_id=max_tor_id,
        min_aggregation_id=min_aggregation_id,
        max_aggregation_id=max_aggregation_id,
        min_core_id=min_core_id,
        max_core_id=max_core_id,
        min_server_id=min_server_id,
        max_server_id=max_server_id,
        uplinks=uplinks,
        downlinks=downlinks,
    )


if __name__ == "__main__":
    for _k in [16, 32, 64, 128, 256]:
        create_3level(k=_k)
        create_3level_with_server(k=_k)
