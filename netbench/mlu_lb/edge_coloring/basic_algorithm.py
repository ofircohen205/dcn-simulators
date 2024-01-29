from networkx import MultiGraph

from mlu_lb.edge_coloring.euler import euler_partitions
from mlu_lb.edge_coloring.matching import find_perfect_matching
from mlu_lb.utils.graph_loader import construct_bipartite_subgraph


def recursive_edge_coloring(bipartite_multigraph: MultiGraph, colors: dict, color: int):
    max_degree = max([bipartite_multigraph.degree(node) for node in bipartite_multigraph.nodes])
    if max_degree % 2 == 1:
        matching = find_perfect_matching(bipartite_multigraph, max_degree)
        edges = list(matching.edges)
        while edges:
            u, v, idx = edges.pop()
            bipartite_multigraph.remove_edge(u, v)
            colors[(u, v)] = color
        color += 1

    partitions = euler_partitions(bipartite_multigraph)
    if len(partitions) > 0:
        list_a, list_b = [], []
        for path in partitions:
            for i, edge in enumerate(path):
                if i % 2 == 0:
                    list_a.append(edge)
                else:
                    list_b.append(edge)

        subgraph_1 = construct_bipartite_subgraph(list_a)
        subgraph_2 = construct_bipartite_subgraph(list_b)

        recursive_edge_coloring(subgraph_1, colors, color)
        recursive_edge_coloring(subgraph_2, colors, color)


def basic_edge_coloring(bipartite_multigraph: MultiGraph) -> dict:
    """
    Basic edge coloring algorithm.
    Runtime complexity: O(|E| * sqrt(|V|) * log(|V|))
    :param bipartite_multigraph:
    :return: dict of colors for each edge
    """
    while True:
        nodes = list(bipartite_multigraph.nodes)
        zero_nodes = [node for node in nodes if bipartite_multigraph.degree(node) == 0]
        if len(zero_nodes) == 0:
            break
        bipartite_multigraph.remove_nodes_from(zero_nodes)

    colors: dict = {}
    color = 0
    recursive_edge_coloring(bipartite_multigraph, colors, color)
    assignments = {}
    for k, v in colors.items():
        src = k[0][:-1] if "-" in k[0] else k[-1][:-1]
        dst = k[-1][:-1] if "-" in k[0] else k[0][:-1]
        assignments[f"{src}-{dst}"] = v
    return assignments
