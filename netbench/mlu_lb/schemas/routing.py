from enum import Enum


class Routing(str, Enum):
    ECMP = "ecmp"
    MCVLC = "mcvlc"
    GREEDY_EDGE_COLORING = "greedy_edge_coloring"
    FAST_EDGE_COLORING = "fast_edge_coloring"
    ONLINE_GREEDY = "online_greedy"
