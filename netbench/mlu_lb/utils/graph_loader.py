import networkx as nx


def construct_bipartite_subgraph(edges: list) -> nx.MultiGraph:
    subgraph = nx.MultiGraph()
    for u, v in edges:
        if "-" in u:
            subgraph.add_node(u, bipartite=0)
        else:
            subgraph.add_node(u, bipartite=1)

        if "+" in v:
            subgraph.add_node(v, bipartite=1)
        else:
            subgraph.add_node(v, bipartite=0)

        subgraph.add_edge(u, v)

    return subgraph


def construct_directed_multigraph(src_dst_pairs: list) -> nx.MultiDiGraph:
    graph = nx.MultiDiGraph()
    graph.add_edges_from(src_dst_pairs)
    return graph


def construct_bipartite_multigraph(graph: nx.DiGraph) -> nx.MultiGraph:
    bipartite_graph = nx.MultiGraph()

    nodes = list(graph.nodes)
    bipartite_graph.add_nodes_from(map(lambda x: f"{x}-", nodes), bipartite=0)
    bipartite_graph.add_nodes_from(map(lambda x: f"{x}+", nodes), bipartite=1)

    for src, dst, idx in graph.edges:
        bipartite_graph.add_edge(f"{src}-", f"{dst}+", idx)

    return bipartite_graph


def load_graph_from_file(topology_fname: str, link_bandwidth: int = 10) -> nx.DiGraph:
    with open(topology_fname, "r") as f:
        graph = nx.DiGraph()

        lines = [line.replace("\n", "") for line in f.readlines()]
        for line in lines:
            if line.startswith("|V|"):
                graph.graph["num_nodes"] = int(line.split("=")[1])
            if line.startswith("|E|"):
                graph.graph["num_edges"] = int(line.split("=")[1])
            if line.startswith("ToRs="):
                _set_attr(graph=graph, line=line, node_type="tor")
            if line.startswith("Cores="):
                _set_attr(graph=graph, line=line, node_type="core")
            if line.startswith("Servers="):
                _set_attr(graph=graph, line=line, node_type="server")
            if line.startswith("Aggregation="):
                _set_attr(graph=graph, line=line, node_type="aggregation")
            if line.startswith("# Uplinks:"):
                start = lines.index("# Uplinks:") + 1
                end = lines.index("# Downlinks:")
                for line in lines[start:end]:
                    link = line.split(" ")
                    src, dst = int(link[0]), int(link[1])
                    graph.add_edge(
                        src,
                        dst,
                        weight=1.0,
                        capacity=link_bandwidth,
                        link_type="uplink",
                    )
            if line.startswith("# Downlinks:"):
                start = lines.index("# Downlinks:") + 1
                end = len(lines)
                for line in lines[start:end]:
                    link = line.split(" ")
                    src, dst = int(link[0]), int(link[1])
                    graph.add_edge(
                        src,
                        dst,
                        weight=1.0,
                        capacity=link_bandwidth,
                        link_type="downlink",
                    )

        return graph


def _set_attr(
    graph: nx.Graph,
    line: str,
    node_type: str,
):
    start, end = (
        line.split("=")[-1]
        .replace("incl_range", "")
        .replace("\n", "")
        .replace("(", "")
        .replace(")", "")
        .split(",")
    )
    node_ids = list(range(int(start), int(end) + 1))
    graph.add_nodes_from(node_ids, layer=node_type)
    graph.graph[f"min_{node_type}_id"] = node_ids[0]
    graph.graph[f"max_{node_type}_id"] = node_ids[-1]


def get_server_ids(graph: nx.Graph) -> set:
    return set(
        range(
            graph.graph["min_server_id"],
            graph.graph["max_server_id"] + 1,
        )
    )


def get_tor_ids(graph: nx.Graph) -> set:
    return set(
        range(
            graph.graph["min_tor_id"],
            graph.graph["max_tor_id"] + 1,
        )
    )


def get_core_ids(graph: nx.Graph) -> set:
    return set(
        range(
            graph.graph["min_core_id"],
            graph.graph["max_core_id"] + 1,
        )
    )


def get_servers_under_tor(graph: nx.Graph):
    return {
        i: set(graph.neighbors(i)).intersection(get_server_ids(graph)) for i in get_tor_ids(graph)
    }


def get_tor_of_host(graph: nx.DiGraph, host_id: int):
    return list(graph.predecessors(host_id))[0]


def split_servers(graph: nx.Graph, num_groups: int):
    servers = get_server_ids(graph)
    group_size = len(servers) // num_groups
    groups = []
    for i in range(0, len(servers), group_size):
        groups.append(list(servers)[i : i + group_size])
    return groups
