package ch.ethz.systems.netbench.core.utility;

public class Constants {

    public static class ScenarioTopology {
        public final static String FILE = "scenario_topology_file";
        public final static String EXTEND_WITH_SERVERS = "scenario_topology_extend_with_servers";
        public final static String EXTEND_SERVERS_PER_TL_NODE = "scenario_topology_extend_servers_per_tl_node";
        public final static String EDGE_CAPACITY = "edge_capacity";
    }

    public static class OutputPortGenerators {
        public final static String OUTPUT_PORT = "output_port";
        public final static String ECN_TAIL_DROP = "ecn_tail_drop";
        public final static String OUTPUT_PORT_META_NODE_EPOCH = "meta_node_epoch";
        public final static String PRIORITY = "priority";
        public final static String BOUNDED_PRIORITY = "bounded_priority";
        public final static String UNLIMITED = "unlimited";
        public final static String OUTPUT_PORT_REMOTE = "remote";
        public final static String LIGHT_PORT = "light_port";
        public final static String MAX_QUEUE_SIZE_BYTES = "output_port_max_queue_size_bytes";
        public final static String ECN_THRESHOLD_K_BYTES = "output_port_ecn_threshold_k_bytes";
    }

    public static class NetworkDeviceIntermediary {
        public final static String NETWORK_DEVICE_INTERMEDIARY = "network_device_intermediary";
        public final static String NETWORK_DEVICE_DEMO = "demo";
        public final static String IDENTITY = "identity";
        public final static String NETWORK_DEVICE_UNIFORM = "uniform";
        public final static String LOW_HIGH_PRIORITY = "low_high_priority";
    }

    public static class NetworkDeviceGenerator {
        public final static String NETWORK_DEVICE = "network_device";
        public final static String FORWARDER_SWITCH = "forwarder_switch";
        public final static String ECMP_SWITCH = "ecmp_switch";
        public final static String RANDOM_VALIANT_ECMP_SWITCH = "random_valiant_ecmp_switch";
        public final static String ECMP_THEN_RANDOM_VALIANT_ECMP_SWITCH = "ecmp_then_random_valiant_ecmp_switch";
        public final static String SOURCE_ROUTING_SWITCH = "source_routing_switch";
        public final static String REMOTE_SOURCE_ROUTING_SWITCH = "remote_source_routing_switch";
        public final static String ECMP_THEN_SOURCE_ROUTING_SWITCH = "ecmp_then_source_routing_switch";
        public final static String HYBRID_OPTIC_ELECTRONIC = "hybrid_optic_electronic";
        public final static String ROTOR_SWITCH = "rotor_switch";
        public final static String DYNAMIC_SWITCH = "dynamic_switch";
        public final static String SEMI_REMOTE_ROUTING_SWITCH = "semi_remote_routing_switch";
        public final static String OPTIC_SERVER = "optic_server";
        public final static String OPERA_SWITCH = "opera_switch";
        public final static String META_NODE_SWITCH = "meta_node_switch";
        public final static String EPOCH_META_NODE_SWITCH = "epoch_meta_node_switch";
        public final static String ALLOW_DUPLICATE_EDGES = "allow_duplicate_edges";
        public final static String ENABLE_JUMBO_FLOWS = "enable_jumbo_flows";
        public final static String USE_DUMMY_SERVERS = "use_dummy_servers";
        public final static String SWITCHING_TIME_NS = "switching_time_ns";
    }

    public static class Link {
        public final static String LINK = "link";
        public final static String PERFECT_SIMPLE = "perfect_simple";
        public final static String BANDWIDTH_BIT_PER_NS = "link_bandwidth_bit_per_ns";
        public final static String DELAY_NS = "link_delay_ns";
        public final static String CONVERSION_BANDWIDTH_BIT_PER_NS = "conversion_link_bandwidth_bit_per_ns";
        public final static String NUM_FAILED_NODES = "num_failed_nodes";
    }

    public static class TransportLayerGenerator {
        public final static String TRANSPORT_LAYER = "transport_layer";
        public final static String TRANSPORT_LAYER_DEMO = "demo";
        public final static String BARE = "bare";
        public final static String TCP = "tcp";
        public final static String LSTF_TCP = "lstf_tcp";
        public final static String SP_TCP = "sp_tcp";
        public final static String SP_HALF_TCP = "sp_half_tcp";
        public final static String PFABRIC = "pfabric";
        public final static String PFZERO = "pfzero";
        public final static String BUFFER_TCP = "buffertcp";
        public final static String DIST_MEAN = "distmean";
        public final static String DIST_RAND = "distrand";
        public final static String SPARK_TCP = "sparktcp";
        public final static String DC_TCP = "dctcp";
        public final static String SIMPLE_TCP = "simple_tcp";
        public final static String SIMPLE_DC_TCP = "simple_dctcp";
        public final static String TRANSPORT_LAYER_REMOTE = "remote";
        public final static String TRANSPORT_LAYER_NULL = "null";
        public final static String SIMPLE_UDP = "simple_udp";
        public final static String META_NODE = "meta_node";
        public final static String TRANSPORT_LAYER_META_NODE_EPOCH = "meta_node_epoch";
        public final static String ENABLE_DISTRIBUTED_TRANSPORT_LAYER = "enable_distributed_transport_layer";

    }

    public static class InfrastructureCreation {
        public final static String VERIFY_LINKS_ON_CREATION = "verify_links_on_creation";
    }

    public static class VertexTieBreakRule {
        public final static String VERTEX_TIE_BREAK_RULE = "vertex_tie_break_rule";
    }

    public static class NetworkDeviceRouting {
        public final static String NETWORK_DEVICE_ROUTING = "network_device_routing";
        public final static String SINGLE_FORWARD = "single_forward";
        public final static String ECMP = "ecmp";
        public final static String K_SHORTEST_PATHS = "k_shortest_paths";
        public final static String ECMP_THEN_K_SHORTEST_PATHS = "ecmp_then_k_shortest_paths";
        public final static String ECMP_THEN_K_SHORTEST_PATHS_WITHOUT_SHORTEST = "ecmp_then_k_shortest_paths_without_shortest";
        public final static String REMOTE_ROUTING_POPULATOR = "remote_routing_populator";
        public final static String META_NODE_ROUTER = "meta_node_router";
        public final static String EPOCH_META_NODE_ROUTER = "epoch_meta_node_router";
        public final static String EMPTY_ROUTING_POPULATOR = "empty_routing_populator";
        public final static String OPERA = "opera";
    }

    public static class RemoteRoutingPopulator {
        public final static String CENTERED_ROUTING_TYPE = "centered_routing_type";
        public final static String HEADER_SIZE = "remote_routing_header_size";
        public final static String SEMI_REMOTE_ROUTING_PATH_DIR = "semi_remote_routing_path_dir";
        public final static String DISTRIBUTED_PROTOCOL_ENABLED = "distributed_protocol_enabled";
    }

    public static class Opera {
        public final static String ROUTING_TABLES_DIR_PATH = "opera_routing_tables_dir_path";
        public final static String ROTORS_DIR_PATH = "opera_rotors_dir_path";
        public final static String PARALLEL_ROTORS_TO_CONFIG = "opera_parallel_rotors_to_config";
        public final static String RECONFIGURATION_TIME_NS = "opera_reconfiguration_time_ns";
        public final static String RECONFIGURATION_EXECUTION_TIME = "opera_reconfiguration_execution_time";
        public final static String DIRECT_CIRCUIT_THRESHOLD_BYTE = "opera_direct_circuit_threshold_byte";
    }

    public static class Traffic {
        public final static String TRAFFIC = "traffic";
        public final static String POISSON_ARRIVAL = "poisson_arrival";
        public final static String TRAFFIC_PAIR = "traffic_pair";
        public final static String TRAFFIC_ARRIVALS_STRING = "traffic_arrivals_string";
        public final static String TRAFFIC_ARRIVALS_FILE = "traffic_arrivals_file";
    }

    public static class TrafficProbabilities {
        public final static String ACTIVE_FRACTION_IS_ORDERED = "traffic_probabilities_active_fraction_is_ordered";
        public final static String ACTIVE_FRACTION = "traffic_probabilities_active_fraction";
        public final static String FRACTION_A = "traffic_probabilities_fraction_A";
        public final static String MASS_A = "traffic_probabilities_mass_A";
    }

    public static class PoissonArrival {
        public final static String TRAFFIC_PROBABILITIES_FILE = "traffic_probabilities_file";
        public final static String TRAFFIC_LAMBDA_FLOW_STARTS_PER_S = "traffic_lambda_flow_starts_per_s";
        public final static String TRAFFIC_FLOW_SIZE_DIST = "traffic_flow_size_dist";
        public final static String TRAFFIC_FLOW_SIZE_DIST_PARETO_SHAPE = "traffic_flow_size_dist_pareto_shape";
        public final static String TRAFFIC_FLOW_SIZE_DIST_PARETO_MEAN_KB = "traffic_flow_size_dist_pareto_mean_kilobytes";
        public final static String TRAFFIC_FLOW_SIZE_DIST_UNIFORM_MEAN_BYTES = "traffic_flow_size_dist_uniform_mean_bytes";
        public final static String TRAFFIC_PROBABILITIES_GENERATOR = "traffic_probabilities_generator";
        public final static String ALL_TO_ALL = "all_to_all";
        public final static String ALL_TO_ONE = "all_to_one";
        public final static String ONE_TO_ALL = "one_to_all";
        public final static String ALL_TO_ALL_FRACTION = "all_to_all_fraction";
        public final static String ALL_TO_ALL_SERVER_FRACTION = "all_to_all_server_fraction";
        public final static String PAIRINGS_FRACTION = "pairings_fraction";
        public final static String SKEW_PARETO_DISTRIBUTION = "skew_pareto_distribution";
        public final static String SKEW_PARETO_DISTRIBUTION_SERVER = "skew_pareto_distribution_server";
        public final static String DUAL_ALL_TO_ALL_FRACTION = "dual_all_to_all_fraction";
        public final static String DUAL_ALL_TO_ALL_SERVER_FRACTION = "dual_all_to_all_server_fraction";
        public final static String FLUID_FLOW = "fluid_flow";
        public final static String FLUID_FLOW_PERM = "fluid_flow_perm";
        public final static String FLUID_FLOW_DENSITY_MATRIX = "fluid_flow_density_matrix";
        public final static String META_NODE_PREMUTATION_TRAFFIC = "meta_node_premutation_traffic";
        public final static String META_NODE_A2A_TRAFFIC = "meta_node_a2a_traffic";
        public final static String MOCK_META_NODE_PREMUTATION_TRAFFIC = "mock_meta_node_premutation_traffic";
        public final static String CSV_SIZE_DIST_FILE_BYTES = "csv_size_dist_file_bytes";
        public final static String TRAFFIC_PARETO_SKEW_SHAPE = "traffic_pareto_skew_shape";

        public static class DistributionName {
            public final static String ORIGINAL_SIMON = "original_simon";
            public final static String PFABRIC_DATA_MINING_LOWER_BOUND = "pfabric_data_mining_lower_bound";
            public final static String PFABRIC_DATA_MINING_UPPER_BOUND = "pfabric_data_mining_upper_bound";
            public final static String PFABRIC_WEB_SEARCH_LOWER_BOUND = "pfabric_web_search_lower_bound";
            public final static String PFABRIC_WEB_SEARCH_UPPER_BOUND = "pfabric_web_search_upper_bound";
            public final static String PARETO = "pareto";
            public final static String ALIBABA_MACHINE_LEARNING = "alibaba_machine_learning";
            public final static String FLOW_SIZE_UNIFORM = "uniform";
            public final static String FROM_CSV = "from_csv";
            public final static String FB_FRONTEND_INTRA_DC_CACHE = "fb_frontend_intra_dc_cache";
            public final static String FB_FRONTEND_INTRA_DC_WEB = "fb_frontend_intra_dc_web";
        }
    }

    public static class TrafficPair {
        public final static String TRAFFIC_PAIR_TYPE = "traffic_pair_type";
        public final static String TRAFFIC_PAIR_FLOW_SIZE_BYTE = "traffic_pair_flow_size_byte";
        public final static String ALL_TO_ALL = "all_to_all";
        public final static String ALL_TO_ONE = "all_to_one";
        public final static String ONE_TO_ALL = "one_to_all";
        public final static String ALL_TO_ONE_TOR = "all_to_one_tor";
        public final static String ONE_TO_ALL_TOR = "one_to_all_tor";
        public final static String ONE_TO_ONE = "one_to_one";
        public final static String STRIDE = "stride";
        public final static String CUSTOM = "custom";
        public final static String TRAFFIC_PAIR_STRIDE = "traffic_pair_stride";
        public final static String TRAFFIC_PAIRS = "traffic_pairs";
    }

    public static class DistributedTraining {
        public final static String DATA_PARALLEL = "data_parallel";
        public final static String PIPELINE_PARALLEL = "pipeline_parallel";
        public final static String HYBRID_PARALLEL = "hybrid_parallel";

        public final static String EPOCHS = "epochs";

        public final static String BASE_TRAFFIC_PAIRS_DIR = "base_traffic_pairs_dir";
    }

    public static class TrafficArrivals {
        public final static String TRAFFIC_ARRIVALS_LIST = "traffic_arrivals_list";
        public final static String TRAFFIC_ARRIVALS_FILE_NAME = "traffic_arrivals_file_name";
    }

    public static class NetworkType {
        public final static String NETWORK_TYPE = "network_type";
        public final static String CIRCUIT_SWITCH = "circuit_switch";
        public final static String PACKET_SWITCH = "packet_switch";
    }

    public static class Spark {
        public final static String SPARK_ERROR_DISTRIBUTION = "spark_error_distribution";
    }

    public static class Simulation {
        public final static String FINISH_WHEN_FIRST_FLOWS_FINISH = "finish_when_first_flows_finish";
        public final static String RUN_FOLDER_BASE_DIR = "run_folder_base_dir";
        public final static String RUN_FOLDER_NAME = "run_folder_name";
        public final static String SEED = "seed";
        public final static String COMMON_RUN_NAME = "common_run_name";
        public final static String ROUTING_SCHEME = "routing_scheme";
        public final static String SOURCE_SERVER = "source_server";
        public final static String SOURCE_SERVERS = "source_servers";
        public final static String SOURCE_TOR = "source_tor";
        public final static String TARGET_SERVER = "target_server";
        public final static String TARGET_SERVERS = "target_servers";
        public final static String TARGET_TOR = "target_tor";
        public final static String RUN_TIME_S = "run_time_s";
        public final static String RUN_TIME_NS = "run_time_ns";
        public final static String FROM_STATE = "from_state";
        public final static String COMMON_BASE_DIR = "common_base_dir";
    }

    public static class FlowSizeEstimation {
        public final static String ESTIMATE_FLOW_SIZE = "estimate_flow_size";
        public final static String ESTIMATE_FLOW_SIZE_LOOKBACK = "estimate_flow_size_lookback";
        public final static String ESTIMATE_FLOW_SIZE_MODEL_PATH = "estimate_flow_size_model_path";
    }

    public static class Valiant {
        public final static String NODE_RANGE_LOWER_INCL = "routing_random_valiant_node_range_lower_incl";
        public final static String NODE_RANGE_UPPER_INCL = "routing_random_valiant_node_range_lower_incl";
        public final static String THRESHOLD_BYTES = "routing_ecmp_then_valiant_switch_threshold_bytes";
    }

    public static class DynamicController {
        public final static String MAX_DYNAMIC_SWITCH_DEGREE = "max_dynamic_switch_degree";
    }

    public static class Optic {
        public final static String NUM_PATHS_TO_RANDOMIZE = "num_paths_to_randomize";
        public final static String STATIC_CONFIG_TIME_NS = "static_configuration_time_ns";
        public final static String CIRCUIT_TEARDOWN_TIMEOUT_NS = "circuit_teardown_timeout_ns";
    }

    public static class MetaNode {
        public final static String MOCK_META_NODE_NUM = "mock_meta_node_num";
        public final static String TRANSPORT_BACKOFF_TIME_NS = "meta_node_transport_backoff_time_ns";
        public final static String EPOCH_TIME = "meta_node_epoch_time";
        public final static String SAME_RACK_TRAFFIC = "meta_node_same_rack_traffic";
        public final static String DEFAULT_TOKEN_SIZE_BYTES = "meta_node_default_token_size_bytes";
        public final static String DEFAULT_SERVER_TOKEN_SIZE_BYTES = "meta_node_default_server_token_size_bytes";
        public final static String TOKEN_TIMEOUT_NS = "meta_node_token_timeout_ns";
        public final static String SERVER_MAX_LOAD_BYTES = "meta_node_server_max_load_bytes";
        public final static String NUM_PAIRS_IN_META_NODE_PERMUTATION = "num_pairs_in_meta_node_permutation";
        public final static String CALC_TRANSFER_TIME_BY = "meta_node_calc_trasfer_time_by";
    }

    public static class KShortestPaths {
        public final static String K_FOR_K_SHORTEST_PATHS = "k_for_k_shortest_paths";
        public final static String K_SHORTEST_PATHS_NUM = "k_shortest_paths_num";
    }

    public static class SourceRoutingSwitch {
        public final static String THRESHOLD_BYTES = "routing_ecmp_then_source_routing_switch_threshold_bytes";
        public final static String ALLOW_SOURCE_ROUTING_SKIP_DUPLICATE_PATHS = "allow_source_routing_skip_duplicate_paths";
        public final static String ALLOW_SOURCE_ROUTING_ADD_DUPLICATE_PATHS = "allow_source_routing_add_duplicate_paths";
    }

    public static class FatTree {
        public final static String DEGREE = "fat_tree_degree";
    }

    public static class Circuit {
        public final static String CIRCUIT_WAVE_LENGTH_NUM = "circuit_wave_length_num";
        public final static String HYBRID_CIRCUIT_THRESHOLD_BYTE = "hybrid_circuit_threshold_byte";
        public final static String MAX_NUM_FLOWS_ON_CIRCUIT = "max_num_flows_on_circuit";
    }

    public static class Rotor {
        public final static String NET_RECONFIGURATION_TIME_NS = "rotor_net_reconfiguration_time_ns";
        public final static String NET_RECONFIGURATION_INTERVAL_NS = "rotor_net_reconfiguration_interval_ns";
        public final static String MAX_BUFFER_SIZE_BYTE = "max_rotor_buffer_size_byte";
        public final static String RANDOM_PORT = "random_rotor_port";
    }

    public static class Logger {
        public final static String ENABLE_LOG_FLOW_THROUGHPUT = "enable_log_flow_throughput";
        public final static String ENABLE_LOG_PORT_QUEUE_STATE = "enable_log_port_queue_state";
        public final static String ENABLE_GENERATE_HUMAN_READABLE_FLOW_COMPLETION_LOG = "enable_generate_human_readable_flow_completion_log";
        public final static String LOG_PORT_UTILIZATION = "log_port_utilization";
        public final static String LOG_REMOTE_PATHS = "log_remote_paths";
        public final static String LOG_REMOTE_ROUTER_STATE = "log_remote_router_state";
        public final static String LOG_REMOTE_ROUTER_DROPS = "log_remote_router_drops";
        public final static String ENABLE_LOG_PACKET_BURST_GAP = "enable_log_packet_burst_gap";
        public final static String ENABLE_LOG_CONGESTION_WINDOW = "enable_log_congestion_window";
    }

    public static class Xpander {
        public final static String SERVERS_INFINITE_CAPACITY = "servers_inifinite_capcacity";
        public final static String HOST_OPTICS_ENABLED = "host_optics_enabled";
        public final static String DIJKSTRA_VERTEX_SHUFFLE = "dijkstra_vertex_shuffle";
        public final static String MAX_PATH_WEIGHT = "maximum_path_weight";
        public final static String PATHS_FILTER = "paths_filter";
        public final static String FILTER_FIRST = "filter_first";
        public final static String BY_LOWER_INDEX = "by_lower_index";
        public final static String LEAST_LOADED_PATH = "least_loaded_path";
        public final static String RANDOM_PATH = "random_path";
        public final static String MOST_LOADED_PATH = "most_loaded_path";
    }

    public static class Flowlet {
        public final static String FLOWLET_GAP_NS = "FLOWLET_GAP_NS";
    }

    public static class UDP {
        public final static String ENABLE_FAIR_UDP = "enable_fair_udp";
    }

    public static class TCP {
        public final static String ROUND_TRIP_TIMEOUT_NS = "TCP_ROUND_TRIP_TIMEOUT_NS";
        public final static String MAX_SEGMENT_SIZE = "TCP_MAX_SEGMENT_SIZE";
        public final static String INITIAL_SLOW_START_THRESHOLD = "TCP_INITIAL_SLOW_START_THRESHOLD";
        public final static String MAX_WINDOW_SIZE = "TCP_MAX_WINDOW_SIZE";
        public final static String LOSS_WINDOW_SIZE = "TCP_LOSS_WINDOW_SIZE";
        public final static String MINIMUM_SSTHRESH = "TCP_MINIMUM_SSTHRESH";
        public final static String INITIAL_WINDOW_SIZE = "TCP_INITIAL_WINDOW_SIZE";
        public final static String MINIMUM_ROUND_TRIP_TIMEOUT = "TCP_MINIMUM_ROUND_TRIP_TIMEOUT";
        public final static String DCTCP_WEIGHT_NEW_ESTIMATION = "DCTCP_WEIGHT_NEW_ESTIMATION";
    }

    public static class FluidFlow {
        public final static String NUM_FLOWS_FOR_PAIR = "fluid_flow_num_flows_for_pair";
    }

    public static class RoutingStrategies {
        public final static String ECMP = "ecmp";
        public final static String EDGE_COLORING = "edge_coloring";
        public final static String MCVLC = "mcvlc";
        public final static String SIMULATED_ANNEALING = "simulated_annealing";
        public final static String ILP_SOLVER = "ilp_solver";
    }
}
