from typing import Tuple, List


def build_tor_core_links(
    min_tor_id: int,
    max_tor_id: int,
    min_core_id: int,
    max_core_id: int,
) -> Tuple[List[Tuple[int, int]], List[Tuple[int, int]]]:
    tors = list(range(min_tor_id, max_tor_id + 1))
    cores = list(range(min_core_id, max_core_id + 1))

    uplinks, downlinks = [], []
    for tor in tors:
        for core in cores:
            uplinks.append((tor, core))
            downlinks.append((core, tor))
    return uplinks, downlinks


def build_server_tor_links(
    min_tor_id: int,
    max_tor_id: int,
    min_server_id: int,
    max_server_id: int,
) -> Tuple[List[Tuple[int, int]], List[Tuple[int, int]]]:
    servers_per_tor = (max_server_id - min_server_id + 1) // (max_tor_id - min_tor_id + 1)
    tors = list(range(min_tor_id, max_tor_id + 1))
    servers = list(range(min_server_id, max_server_id + 1))

    servers_by_tors = [
        servers[i : i + servers_per_tor] for i in range(0, len(servers), servers_per_tor)
    ]

    uplinks, downlinks = [], []
    for tor in tors:
        for server in servers_by_tors[tor]:
            uplinks.append((server, tor))
            downlinks.append((tor, server))
    return uplinks, downlinks


def build_tor_aggregation_links(
    min_tor_id: int,
    max_tor_id: int,
    min_aggregation_id: int,
    max_aggregation_id: int,
    num_pods: int,
) -> Tuple[List[Tuple[int, int]], List[Tuple[int, int]]]:
    nodes_per_pod = (max_tor_id - min_tor_id + 1) // num_pods
    tors = list(range(min_tor_id, max_tor_id + 1))
    aggregations = list(range(min_aggregation_id, max_aggregation_id + 1))

    tors_by_pods = [tors[i : i + nodes_per_pod] for i in range(0, len(tors), nodes_per_pod)]
    aggregations_by_pods = [
        aggregations[i : i + nodes_per_pod] for i in range(0, len(aggregations), nodes_per_pod)
    ]
    uplinks, downlinks = [], []
    for pod_id in range(num_pods):
        pod_tors = tors_by_pods[pod_id]
        pod_aggregations = aggregations_by_pods[pod_id]
        for tor in pod_tors:
            for aggregation in pod_aggregations:
                uplinks.append((tor, aggregation))
                downlinks.append((aggregation, tor))
    return uplinks, downlinks


def build_aggregation_core_links(
    min_aggregation_id: int,
    max_aggregation_id: int,
    min_core_id: int,
    max_core_id: int,
    num_pods: int,
) -> Tuple[List[Tuple[int, int]], List[Tuple[int, int]]]:
    nodes_per_pod = (max_aggregation_id - min_aggregation_id + 1) // num_pods
    aggregations = list(range(min_aggregation_id, max_aggregation_id + 1))
    cores = list(range(min_core_id, max_core_id + 1))

    aggregations_by_pods = [
        aggregations[i : i + nodes_per_pod] for i in range(0, len(aggregations), nodes_per_pod)
    ]
    core_by_pods = [cores[i : i + nodes_per_pod] for i in range(0, len(cores), nodes_per_pod)]
    uplinks, downlinks = [], []
    for pod_id in range(num_pods):
        pod_aggregations = aggregations_by_pods[pod_id]
        for i, aggregation in enumerate(pod_aggregations):
            for core in core_by_pods[i]:
                uplinks.append((aggregation, core))
                downlinks.append((core, aggregation))
    return uplinks, downlinks
