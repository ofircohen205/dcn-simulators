from os import getcwd
import os.path as osp

from mlu_lb.topologies_generator.links_generator import build_tor_core_links, build_server_tor_links
from mlu_lb.topologies_generator.topology_writer import write_file


def create_2level(k: int):
    assert k % 2 == 0, "k must be even"
    num_tors = k
    num_cores = k // 2
    topology_file = osp.join(
        getcwd(),
        "topologies",
        "fat_tree",
        "2_level",
        f"fat_tree-{num_tors}_tors-{num_cores}_cores.topology",
    )

    total_switches = num_cores + num_tors
    total_nodes = total_switches

    min_tor_id, max_tor_id = 0, num_tors - 1
    min_core_id, max_core_id = num_tors, total_switches - 1
    tor_core_uplinks, tor_core_downlinks = build_tor_core_links(
        min_tor_id=min_tor_id,
        max_tor_id=max_tor_id,
        min_core_id=min_core_id,
        max_core_id=max_core_id,
    )

    uplinks = tor_core_uplinks
    downlinks = tor_core_downlinks
    write_file(
        topology_file=topology_file,
        total_nodes=total_nodes,
        total_links=len(uplinks + downlinks),
        min_tor_id=min_tor_id,
        max_tor_id=max_tor_id,
        min_core_id=min_core_id,
        max_core_id=max_core_id,
        min_server_id=-1,
        max_server_id=-1,
        uplinks=uplinks,
        downlinks=downlinks,
    )


def create_2level_with_servers(k: int):
    assert k % 2 == 0, "k must be even"
    num_tors = k
    num_cores = k // 2
    num_servers = 2 * (k // 2) ** 2
    topology_file = osp.join(
        getcwd(),
        "topologies",
        "fat_tree",
        "2_level",
        f"fat_tree-{num_tors}_tors-{num_cores}_cores-{num_servers}_servers.topology",
    )

    total_switches = num_cores + num_tors
    total_nodes = total_switches + num_servers

    min_tor_id, max_tor_id = 0, num_tors - 1
    min_core_id, max_core_id = num_tors, total_switches - 1
    tor_core_uplinks, tor_core_downlinks = build_tor_core_links(
        min_tor_id=min_tor_id,
        max_tor_id=max_tor_id,
        min_core_id=min_core_id,
        max_core_id=max_core_id,
    )

    min_server_id, max_server_id = total_switches, total_nodes - 1
    tor_server_uplinks, tor_server_downlinks = build_server_tor_links(
        min_tor_id=min_tor_id,
        max_tor_id=max_tor_id,
        min_server_id=min_server_id,
        max_server_id=max_server_id,
    )

    uplinks = tor_core_uplinks + tor_server_uplinks
    downlinks = tor_core_downlinks + tor_server_downlinks
    write_file(
        topology_file=topology_file,
        total_nodes=total_nodes,
        total_links=len(uplinks + downlinks),
        min_tor_id=min_tor_id,
        max_tor_id=max_tor_id,
        min_core_id=min_core_id,
        max_core_id=max_core_id,
        min_server_id=min_server_id,
        max_server_id=max_server_id,
        uplinks=uplinks,
        downlinks=downlinks,
    )


def create_custom_2level_with_servers():
    num_tors = 16
    num_cores = 16
    num_servers = 16 * 8
    topology_file = osp.join(
        getcwd(),
        "topologies",
        "fat_tree",
        "2_level",
        f"fat_tree-{num_tors}_tors-{num_cores}_cores-{num_servers}_servers.topology",
    )

    total_switches = num_cores + num_tors
    total_nodes = total_switches + num_servers

    min_tor_id, max_tor_id = 0, num_tors - 1
    min_core_id, max_core_id = num_tors, total_switches - 1
    tor_core_uplinks, tor_core_downlinks = build_tor_core_links(
        min_tor_id=min_tor_id,
        max_tor_id=max_tor_id,
        min_core_id=min_core_id,
        max_core_id=max_core_id,
    )

    min_server_id, max_server_id = total_switches, total_nodes - 1
    tor_server_uplinks, tor_server_downlinks = build_server_tor_links(
        min_tor_id=min_tor_id,
        max_tor_id=max_tor_id,
        min_server_id=min_server_id,
        max_server_id=max_server_id,
    )

    uplinks = tor_core_uplinks + tor_server_uplinks
    downlinks = tor_core_downlinks + tor_server_downlinks
    write_file(
        topology_file=topology_file,
        total_nodes=total_nodes,
        total_links=len(uplinks + downlinks),
        min_tor_id=min_tor_id,
        max_tor_id=max_tor_id,
        min_core_id=min_core_id,
        max_core_id=max_core_id,
        min_server_id=min_server_id,
        max_server_id=max_server_id,
        uplinks=uplinks,
        downlinks=downlinks,
    )


if __name__ == "__main__":
    # create_custom_2level_with_servers()
    for _k in [32, 64, 128, 256]:
        # create_2level(k=_k)
        create_2level_with_servers(k=_k)
