# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

Hard part: one consistent view when cost is spread across labour, inventory, transport, overhead and systems don't align. Need clear rules—what belongs to which cost centre, how to split shared/regional cost. Allocation keys (headcount, m², throughput) only work if finance agrees and we review them. Key questions: Who owns cost-centre and allocation definitions? How far back do we keep history?

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

Levers: labour, inventory, transport, space/utilities. Find opportunities from data (spend, trends) and process (bottlenecks, rework). Prioritise by impact vs effort vs effect on service. Run a few pilots first with clear success criteria. Questions: What service metrics are non-negotiable? Who signs off on trade-offs (e.g. lead time vs cost)?

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

One source of truth for finance, fewer rekey errors, faster close, drill-down from P&L to site. Design: clear contract (APIs/files, semantics), idempotency and reconciliation so retries are safe, simple audit trail. Questions: Who's master—Cost Control Tool or ERP? Real-time or batch OK? Latency tolerance?

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

Helps plan capacity, set targets, catch deviations early. Factor in seasonality, volume/mix, capacity, external costs. System needs: history by warehouse/cost/period, budget by centre and period, simple forecast (e.g. rolling or driver-based), actual vs budget vs prior year with drill-down. Questions: Horizon (12–18 months)? Who owns budget and approval? Single baseline or multiple scenarios?

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

Keep history for audit, compliance, and true site performance. New warehouse = new budget and baseline so we can measure improvement and stay on target. Design: archived records read-only and time-bound; reporting filters active vs archived so we don't mix or double-count. Questions: Retention for archived data? Report business unit as one series over time or split by lifecycle?

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
