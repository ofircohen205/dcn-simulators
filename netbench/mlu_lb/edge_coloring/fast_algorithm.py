from os.path import dirname, join
from subprocess import check_call, Popen, PIPE


def fast_edge_coloring(src_dst_pairs: list, output_folder: str) -> dict:
    nodes = set()
    for src, dst in src_dst_pairs:
        nodes.add(src)
        nodes.add(dst)
    nodes_mapping = {node: i for i, node in enumerate(nodes)}
    edges = [(nodes_mapping[src], nodes_mapping[dst]) for src, dst in src_dst_pairs]

    cpp_file = join(dirname(__file__), "fast_edge_coloring.cpp")
    output_file = join(output_folder, "output")
    check_call(["g++", "-std=c++11", "-o", output_file, cpp_file])
    # check_call(["clang++", cpp_file, "output", "-stdlib=libc++"])

    # inputs to the C++ program, as a string
    inputs = f"{len(nodes)} {len(nodes)} {len(src_dst_pairs)}\n"
    for u, v in edges:
        inputs += f"{u} {v}\n"

    # run the executable, and provide input
    process = Popen("./output", stdin=PIPE, stdout=PIPE)
    stdout, stderr = process.communicate(input=inputs.encode())

    # parse the output
    assignments = {}
    output = list(map(int, stdout.decode("utf-8").split("\n")[:-1]))
    for (src, dst), color in zip(src_dst_pairs, output):
        assignments[f"{src}-{dst}"] = color

    return assignments
