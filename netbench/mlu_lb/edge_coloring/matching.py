from networkx.algorithms.bipartite import hopcroft_karp_matching
from networkx import MultiGraph


def find_perfect_matching(bipartite_multigraph: MultiGraph, max_degree: int) -> MultiGraph:
    """
    Find perfect matching in bipartite multigraph using Hopcroft-Karp algorithm.
    Runtime complexity: O(|E| * sqrt(|V|))
    :param bipartite_multigraph:
    :param max_degree:
    :return:
    """
    if max_degree == 1:
        return bipartite_multigraph

    left_vertices = [
        v for v, data in bipartite_multigraph.nodes(data=True) if data["bipartite"] == 0
    ]
    right_vertices = [
        v for v, data in bipartite_multigraph.nodes(data=True) if data["bipartite"] == 1
    ]

    matchings = []
    for i, vertices in enumerate([left_vertices, right_vertices]):
        vertices_T = [v for v in vertices if bipartite_multigraph.degree(v) < max_degree]
        vertices_H = [v for v in vertices if v not in vertices_T]
        top_nodes = right_vertices if i == 0 else left_vertices
        vertices_H.extend(top_nodes)
        multigraph_H = bipartite_multigraph.subgraph(vertices_H)
        matching_H = hopcroft_karp_matching(multigraph_H, top_nodes)
        result = set()
        for u, v in matching_H.items():
            result.add(frozenset({u, v}))
        matchings.append(result)

    matching_M: set = matchings[0].intersection(matchings[1])
    matching_N: set = matchings[0].__xor__(matchings[1])
    for cc in matching_N:
        vertices = list(cc)
        edges = [(vertices[i], vertices[i + 1]) for i in range(len(vertices) - 1)]
        for edge in edges:
            matching_M.add(frozenset(edge))
    matching = MultiGraph()
    for edge in matching_M:
        u, v = list(edge)
        matching.add_edge(u, v)

    return matching
