from networkx import MultiGraph


def euler_partitions(bipartite_multigraph: MultiGraph) -> list:
    partitions = []
    queue = []

    odd_degree_nodes = [
        node for node in bipartite_multigraph.nodes if bipartite_multigraph.degree(node) % 2 == 1
    ]
    queue.extend(odd_degree_nodes)
    even_degree_nodes = [
        node for node in bipartite_multigraph.nodes if bipartite_multigraph.degree(node) % 2 == 0
    ]
    queue.extend(even_degree_nodes)

    while queue:
        s = queue.pop(0)
        if bipartite_multigraph.degree(s) == 0:
            continue

        path = []
        v = s
        while bipartite_multigraph.degree(v) > 0:
            w = next(bipartite_multigraph.neighbors(v))
            bipartite_multigraph.remove_edge(v, w)
            path.append((v, w))
            v = w
        partitions.append(path)

        if bipartite_multigraph.degree(s) > 0:
            queue.append(s)

    return partitions
