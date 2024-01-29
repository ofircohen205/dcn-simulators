from gurobipy import Model, GRB


def solve_ilp(model: Model):
    model.optimize()
    model.setParam(GRB.Param.OutputFlag, 0)

    status = model.Status
    if status in {GRB.INF_OR_UNBD, GRB.INFEASIBLE, GRB.UNBOUNDED}:
        raise ValueError("Model is infeasible or unbounded")

    if status != GRB.OPTIMAL:
        raise ValueError(f"Optimization ended with an error {status}")
