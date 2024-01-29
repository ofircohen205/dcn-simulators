import os.path as osp
from os import getcwd

import networkx as nx
from gurobipy import Model, GRB, quicksum

from mlu_lb.utils.graph_loader import (
    load_graph_from_file,
    get_server_ids,
    get_tor_ids,
    get_tor_of_host,
)


class SingletonMeta(type):
    _instances: dict = {}

    def __call__(cls, *args, **kwargs):
        if cls not in cls._instances:
            cls._instances[cls] = super().__call__(*args, **kwargs)
        return cls._instances[cls]


class CentralizedController(metaclass=SingletonMeta):
    """
    Singleton class of Centralized controller with a global view of the network.
    """

    VLINK_ID = 0

    def __init__(self, level: int, tors: int, cores: int, servers: int):
        self._graph = load_graph_from_file(
            topology_fname=osp.join(
                getcwd(),
                "topologies",
                "fat_tree",
                f"{level}_level",
                f"fat_tree-{tors}_tors-{cores}_cores-{servers}_servers.topology",
            ),
        )
        self._nodes = sorted(self._graph.nodes(data=True))
        server_ids = get_server_ids(graph=self.graph)
        self.graph.graph["servers_under_tor"] = len(server_ids) // len(
            get_tor_ids(graph=self.graph)
        )
        self.non_host_edges = [
            (u, v) for u, v in self.graph.edges if not (u in server_ids or v in server_ids)
        ]

        self._shortest_paths: dict = {}
        self._failed_links: list = []
        self._edges_containing_paths: dict = {edge: [] for edge in self.non_host_edges}

        self._ilp_edges_variables = {(u, v): f"x_e[{u},{v}]" for u, v in self.non_host_edges}
        self._ilp_paths_variables: dict = {}
        self._ilp_paths_mapping: dict = {}
        self._vrings_vlinks_mapping: dict = {}

    def vlink_shortest_paths(self, vlink: tuple):
        return self.shortest_paths.get(vlink, [])

    def to_virtual_links(self, virtual_rings_servers: tuple):
        for ring_id, ring_servers in enumerate(virtual_rings_servers):
            self.vrings_vlinks_mapping[ring_id] = []
            for virtual_link in ring_servers:
                src_tor = get_tor_of_host(self.graph, virtual_link[0])
                dst_tor = get_tor_of_host(self.graph, virtual_link[1])
                self.vrings_vlinks_mapping[ring_id].append(self.VLINK_ID)
                self.update_shortest_paths(vlink=(src_tor, dst_tor))

    def update_shortest_paths(self, vlink: tuple):
        if vlink not in self.shortest_paths:
            paths = [
                list(zip(path[:-1], path[1:])) for path in nx.all_shortest_paths(self.graph, *vlink)
            ]
            self.shortest_paths[vlink] = paths
        self.ilp_paths_variables[self.VLINK_ID] = []
        for i, path in enumerate(self.vlink_shortest_paths(vlink)):
            self.ilp_paths_variables[self.VLINK_ID].append(f"x_p_l_{self.VLINK_ID}[{i}]")
            for edge in path:
                self.edges_containing_paths[edge].append(f"x_p_l_{self.VLINK_ID}[{i}]")
        self.ilp_paths_mapping[self.VLINK_ID] = vlink
        self.VLINK_ID += 1

    def construct_model(self) -> Model:
        r"""
        Objective function for the Min Max Link Fairness problem

        :math:`\min \alpha`

        Subject to :math:`\sum_{p \in P_l} x_p^l = 1 \forall l \in L`

        :math:`\sum_{l \in L} \sum_{p \in P_l : e \in p} x_p^l \leq x_e \forall e \in E`

        :math:`x_e \leq \alpha \forall e \in E`

        :math:`x_p^l = 0 \forall l \in L, p \in P_l : e \in p \land e \in F`

        :math:`x_p^l \in \{0,1\} \forall p \in P_l, l \in L`

        :math:`x_e \in \mathbb{N}^+ \forall e \in E`

        :math:`\alpha \in \mathbb{N}^+`

        :return: The Min-Max-Link-Fairness problem
        """
        model = Model(name="Minimize-Maximum-Link-Fairness")
        # model.setParam(GRB.Param.PoolSolutions, 10)
        # model.setParam(GRB.Param.PoolSearchMode, 2)
        model.setParam(GRB.Param.LogToConsole, 0)

        # Decision variables
        virtual_links_vars = {
            key: model.addVars(
                range(len(self.ilp_paths_variables[key])),
                name=f"x_p_l_{key}",
                vtype=GRB.BINARY,
            )  # x_p^l
            for key in self.ilp_paths_variables
        }
        x_e = model.addVars(self.non_host_edges, vtype=GRB.INTEGER, name="x_e")  # x_e
        alpha = model.addVar(vtype=GRB.INTEGER, name="alpha")  # alpha
        model.update()

        # Define the objective function
        model.setObjective(expr=alpha, sense=GRB.MINIMIZE)

        # Virtual link traverse through one path only constraints
        model.addConstrs(
            (quicksum(virtual_links_vars[key].values()) == 1 for key in self.ilp_paths_variables),
            name="single_path_vlink",
        )

        # Failed links constraints
        model.addConstrs(
            (
                virtual_links_vars[key][path_id] == 0
                for key in self.ilp_paths_variables
                for failed_link in self.failed_links
                for path_id, path in enumerate(
                    self.vlink_shortest_paths(self.ilp_paths_mapping[key])
                )
                if failed_link in path
            ),
            name="failed_links",
        )

        # Min Max Link Fairness constraints
        model.addConstrs(
            (
                quicksum([model.getVarByName(name) for name in self.edges_containing_paths[(u, v)]])
                <= x_e[u, v]
                for u, v in self.non_host_edges
            ),
            name="min_max_link_fairness",
        )

        model.addConstrs((x_e[u, v] <= alpha for u, v in self.non_host_edges), name="alpha_max")
        return model

    def cleanup(self):
        self.ilp_paths_variables = {}
        self.ilp_paths_mapping = {}
        self.edges_containing_paths = {edge: [] for edge in self.non_host_edges}
        self.vrings_vlinks_mapping = {}
        self.VLINK_ID = 0

    @property
    def graph(self):
        return self._graph

    @property
    def nodes(self):
        return self._nodes

    @property
    def failed_links(self):
        return self._failed_links

    @failed_links.setter
    def failed_links(self, failed_links: list):
        self._failed_links = failed_links

    @property
    def ilp_paths_variables(self):
        return self._ilp_paths_variables

    @ilp_paths_variables.setter
    def ilp_paths_variables(self, ilp_paths_variables: dict):
        self._ilp_paths_variables = ilp_paths_variables

    @property
    def ilp_paths_mapping(self):
        return self._ilp_paths_mapping

    @ilp_paths_mapping.setter
    def ilp_paths_mapping(self, ilp_paths_mapping: dict):
        self._ilp_paths_mapping = ilp_paths_mapping

    @property
    def ilp_edges_variables(self):
        return self._ilp_edges_variables

    @property
    def vrings_vlinks_mapping(self):
        return self._vrings_vlinks_mapping

    @vrings_vlinks_mapping.setter
    def vrings_vlinks_mapping(self, vrings_vlinks_mapping: dict):
        self._vrings_vlinks_mapping = vrings_vlinks_mapping

    @property
    def shortest_paths(self):
        return self._shortest_paths

    @property
    def edges_containing_paths(self):
        return self._edges_containing_paths

    @edges_containing_paths.setter
    def edges_containing_paths(self, edges_containing_paths: dict):
        self._edges_containing_paths = edges_containing_paths
