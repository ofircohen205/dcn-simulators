CODE_DIRS=external

green_terminal_output=`tput setaf 2`
reset_terminal_output=`tput sgr0`

ring_sizes=2 4 8
num_concurrent_jobs=1 2 3 4 5
num_core_failures=0 1 4 8
algorithms=ecmp mcvlc edge_coloring simulated_annealing lp_solver
centralized_controller_algorithms=mcvlc edge_coloring simulated_annealing lp_solver

install-python-requirements:
	@echo "${green_terminal_output}Installing requirements...${reset_terminal_output}"
	poetry install
	@echo "${green_terminal_output}Finished installing requirements...${reset_terminal_output}"

compile-maven:
	@echo "${green_terminal_output}Compiling maven...${reset_terminal_output}"
	mvn clean compile assembly:single; mv target/floodns-*-jar-with-dependencies.jar floodns-basic-sim.jar
	@echo "${green_terminal_output}Finished compiling maven...${reset_terminal_output}"

format:
	poetry run black -l 100 ${CODE_DIRS}

lint:
	poetry run ruff ${CODE_DIRS} --fix
	poetry run mypy --install-types ${CODE_DIRS}
	poetry run black --check -l 100 ${CODE_DIRS}

run-basic-sim:
	java -jar floodns-basic-sim.jar $(FOLDER)

####################################
# Simulation of different algorithms
####################################
simulate-jobs:
	for alg in $(algorithms); do \
  		echo "${green_terminal_output}Starting simulation of $$alg...${reset_terminal_output}"; \
		java -jar floodns-basic-sim.jar ./runs/concurrent_jobs_$(JOBS)/$(CORES)_core_failures/ring_size_$(RING)/$$alg; \
		echo "${green_terminal_output}Finished simulation of $$alg...${reset_terminal_output}"; \
	done

simulate-jobs-single-alg:
	java -jar floodns-basic-sim.jar ./runs/concurrent_jobs_$(JOBS)/$(CORES)_core_failures/ring_size_$(RING)/$(ALG)

##################################
# Analysis of different algorithms
##################################
convert-human-readable-concurrent-jobs:
	for alg in $(algorithms); do \
  		for num_cores in $(num_core_failures); do \
			for ring_size in $(ring_sizes); do \
				echo "${green_terminal_output}Starting analysis of $$alg...${reset_terminal_output}"; \
				poetry run python -m external.analysis.convert_to_human_readable concurrent-jobs $$alg $(JOBS) $$num_cores $$ring_size; \
				echo "${green_terminal_output}Finished analysis of $$alg...${reset_terminal_output}"; \
			done \
		done \
	done

####################################
# Comparison of different algorithms
####################################
compare-routing-strategies-concurrent-jobs:
	for num_cores in $(num_core_failures); do \
		for ring_size in $(ring_sizes); do \
			echo "${green_terminal_output}Starting comparing various routing strategies...${reset_terminal_output}"; \
			poetry run python -m external.analysis.routing_strategy_comparison $(JOBS) $$num_cores $$ring_size; \
			echo "${green_terminal_output}Finished comparing various routing strategies...${reset_terminal_output}"; \
		done \
	done


################
# CDF Generation
################
generate-controller-computation-cdf:
	for alg in $(centralized_controller_algorithms); do \
		poetry run python -m external.cdf_generation.controller_computation $$alg; \
	done

generate-cdfs:
	for alg in $(algorithms); do \
		poetry run python -m external.cdf_generation.data_parallelism_time $$alg; \
		poetry run python -m external.cdf_generation.fct_throughput $$alg; \
	done
