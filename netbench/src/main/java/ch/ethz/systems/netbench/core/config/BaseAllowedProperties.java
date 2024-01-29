package ch.ethz.systems.netbench.core.config;

public class BaseAllowedProperties {

    public static final String[] LOG = new String[]{
            "enable_log_port_queue_state",
            "enable_log_flow_throughput",
            "enable_generate_human_readable_flow_completion_log",
            "log_remote_paths",
            "log_remote_router_state",
            "log_remote_router_drops",
            "log_port_utilization"
    };
    public static final String[] PROPERTIES_RUN = new String[]{

            // General
            "scenario_topology_file",
            "scenario_topology_extend_with_servers",
            "scenario_topology_extend_servers_per_tl_node",
            "seed",
            "run_time_s",
            "run_time_ns",
            "run_folder_name",
            "run_folder_base_dir",
            "analysis_command",
            "finish_when_first_flows_finish",
            "from_state",
            // Infrastructure
            "transport_layer",
            "network_device",
            "network_device_intermediary",
            "output_port",
            "link",
            "switching_time_ns",

            // Routing
            "network_device_routing",

            // Traffic
            "traffic",
            "traffic_flow_size_dist",
            "traffic_probabilities_file",
            "traffic_probabilities_generator",
            "traffic_probabilities_active_fraction",
            "traffic_probabilities_active_fraction_is_ordered",
            "traffic_lambda_flow_starts_per_s"

    };
    public static final String[] RUN_CONFIGURATION_ONLY = new String[]{
            "run_folder_name",
            "run_folder_base_dir",
            "analysis_command",
            "finish_when_first_flows_finish",
            "from_state",
            "sub_configurations_folder",
            "common_base_dir",
            "common_run_name",
            "semi_remote_routing_path_dir"
    };
    public static final String[] EXTENSION = new String[]{

            // Basic
            "output_port_max_queue_size_bytes",
            "output_port_ecn_threshold_k_bytes",
            "link_delay_ns",
            "link_bandwidth_bit_per_ns",
            "allow_duplicate_edges",
            // Poisson traffic
            "traffic_pair_type",
            "traffic_pair_flow_size_byte",
            "traffic_pairs",
            "traffic_arrivals_list",
            "traffic_flow_size_dist_uniform_mean_bytes",
            "traffic_flow_size_dist_pareto_shape",
            "traffic_flow_size_dist_pareto_mean_kilobytes",
            "traffic_pareto_skew_shape",
            "traffic_arrivals_file_name",

            // Flowlet
            "FLOWLET_GAP_NS",

            // VLB
            "routing_random_valiant_node_range_lower_incl",
            "routing_random_valiant_node_range_upper_incl",
            "routing_ecmp_then_valiant_switch_threshold_bytes",

            "traffic_probabilities_fraction_A",
            "traffic_probabilities_mass_A",

    };
    public static final String[] EXPERIMENTAL = new String[]{

            "enable_fair_udp",
            "opera_direct_circuit_threshold_byte",
            "opera_rotor_guard_time",
            "opera_direct_circuit_threshold",
            "opera_reconfiguration_execution_time",
            "opera_reconfiguration_time_ns",
            "opera_parallel_rotors_to_config",
            "opera_rotors_dir_path",
            "opera_routing_tables_dir_path",
            "conversion_link_bandwidth_bit_per_ns",
            "fluid_flow_num_flows_for_pair",
            "modulation_frequency",
            "use_dummy_servers",
            "enable_distributed_transport_layer",
            "prioritize_acks_on_circuit",
            "circuit_teardown_timeout_ns",
            "static_configuration_time_ns",
            "distributed_protocol_enabled",
            "csv_size_dist_file_bytes",
            "host_optics_enabled",
            "enable_jumbo_flows",
            "empty_property",
            "max_num_flows_on_circuit",
            "circuit_wave_length_num",
            "num_paths_to_randomize",
            "servers_inifinite_capcacity",
            "semi_remote_routing_path_dir",
            "edge_capacity",
            "verify_links_on_creation",
            "max_rotor_buffer_size_byte",
            "rotor_net_reconfiguration_interval_ns",
            "rotor_net_reconfiguration_time_ns",
            "max_dynamic_switch_degree",
            "hybrid_circuit_threshold_byte",
            "network_type",
            "dijkstra_vertex_shuffle",
            "fat_tree_degree",
            "no_queues_in_servers",
            "base_dir_variants",
            "k_shortest_paths",
            "maximum_path_weight",
            "k_shortest_paths_num",
            "vertex_tie_break_rule",
            "paths_filter",
            "sub_configurations_folder",
            "common_base_dir",
            "common_run_name",
            "path_algorithm",
            "graph_edge_weight_rule",
            // TCP
            "TCP_ROUND_TRIP_TIMEOUT_NS",
            "TCP_MAX_SEGMENT_SIZE",
            "TCP_MAX_WINDOW_SIZE",
            "TCP_LOSS_WINDOW_SIZE",
            "TCP_INITIAL_SLOW_START_THRESHOLD",
            "TCP_INITIAL_WINDOW_SIZE",
            "TCP_MINIMUM_SSTHRESH",
            "TCP_MINIMUM_ROUND_TRIP_TIMEOUT",
            "DCTCP_WEIGHT_NEW_ESTIMATION",
            "enable_log_congestion_window",
            "enable_log_packet_burst_gap",
            "centered_routing_type",
            "remote_routing_header_size",
            // K-shortest-paths
            "k_for_k_shortest_paths",

            // K-paths
            // "k_paths_k_threshold",
            // "k_paths_file",
            "allow_source_routing_skip_duplicate_paths",
            "allow_source_routing_add_duplicate_paths",

            "spark_error_distribution",
            "routing_ecmp_then_source_routing_switch_threshold_bytes",
    };

    public static final String[] GREEDY_FLOW_SCHEDULING = new String[]{
            // Flow scheduling project
            "estimate_flow_size",
            "estimate_flow_size_lookback",
            "estimate_flow_size_model_path",
            "routing_scheme",
            "source_server",
            "target_server",
            "source_tor",
            "target_tor",
            "generate_flow_after_finished",
            "epochs",
            "base_traffic_pairs_dir",
            "num_failed_nodes",
    };
    public static final String[] META_NODE = new String[]{
            "meta_node_epoch_time",
            "meta_node_calc_trasfer_time_by",
            "meta_node_same_rack_traffic",
            "mock_meta_node_num",
            "num_pairs_in_meta_node_permutation",
            "meta_node_default_server_token_size_bytes",
            "meta_node_default_token_size_bytes",
            "meta_node_token_timeout_ns",
            "meta_node_transport_backoff_time_ns",
            "meta_node_server_max_load_bytes"
    };
    public static final String[] BASE_DIR_VARIANTS = new String[]{
            "base_lambda_flow_starts_per_s",
            "variant_graph_name"
    };

    private BaseAllowedProperties() {
        // Private constructor, cannot be constructed
    }

}
