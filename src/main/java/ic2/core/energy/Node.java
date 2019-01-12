package ic2.core.energy;

import ic2.api.energy.NodeStats;
import ic2.api.energy.tile.IEnergyConductor;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.core.IC2;
import ic2.core.util.LogCategory;
import ic2.core.util.Util;

import java.util.ArrayList;
import java.util.List;

class Node
{
	final int uid;
	final Tile tile;
	final NodeType nodeType;
	private final Node parent;
	private boolean isExtraNode = false;
	private int tier;
	private double amount;
	private double resistance;
	private double voltage;
	private double currentIn;
	private double currentOut;
	private Grid grid;
	List<NodeLink> links = new ArrayList<>();
	private final MutableNodeStats lastNodeStats = new MutableNodeStats();

	Node(EnergyNetLocal energyNet, Tile tile1, NodeType nodeType1)
	{
		if (energyNet == null)
			throw new NullPointerException("The energyNet parameter must not be null.");
		if (tile1 == null)
			throw new NullPointerException("The tile parameter must not be null.");
		assert nodeType1 != NodeType.Conductor || tile1.entity instanceof IEnergyConductor;

		assert nodeType1 != NodeType.Sink || tile1.entity instanceof IEnergySink;

		assert nodeType1 != NodeType.Source || tile1.entity instanceof IEnergySource;

		this.uid = EnergyNetLocal.getNextNodeUid();
		this.tile = tile1;
		this.nodeType = nodeType1;
		this.parent = null;
	}

	Node(Node node)
	{
		this.uid = node.uid;
		this.tile = node.tile;
		this.nodeType = node.nodeType;
		this.parent = node;

		assert this.nodeType != NodeType.Conductor || this.tile.entity instanceof IEnergyConductor;

		assert this.nodeType != NodeType.Sink || this.tile.entity instanceof IEnergySink;

		assert this.nodeType != NodeType.Source || this.tile.entity instanceof IEnergySource;

		/* TODO gamerforEA code replace, old code:
		for (NodeLink link : node.links)
		{
			assert link.getNeighbor(node).links.contains(link);
			this.links.add(new NodeLink(link));
		} */
		List<NodeLink> links = node.links;
		for (int i = 0, linksSize = links.size(); i < linksSize; i++)
		{
			NodeLink link = links.get(i);
			assert link.getNeighbor(node).links.contains(link);
			this.links.add(new NodeLink(link));
		}
		// TODO gamerforEA code end
	}

	double getInnerLoss()
	{
		switch (this.nodeType)
		{
			case Source:
				return 0.4D;
			case Sink:
				return 0.4D;
			case Conductor:
				return ((IEnergyConductor) this.tile.entity).getConductionLoss();
			default:
				throw new RuntimeException("invalid nodetype: " + this.nodeType);
		}
	}

	boolean isExtraNode()
	{
		return this.getTop().isExtraNode;
	}

	void setExtraNode(boolean isExtraNode)
	{
		if (this.nodeType == NodeType.Conductor)
			throw new IllegalStateException("A conductor can\'t be an extra node.");
		this.getTop().isExtraNode = isExtraNode;
	}

	int getTier()
	{
		return this.getTop().tier;
	}

	void setTier(int tier)
	{
		if (tier >= 0 && !Double.isNaN((double) tier))
		{
			if (tier > 20 && (tier != Integer.MAX_VALUE || this.nodeType != NodeType.Sink))
			{
				if (Util.inDev())
					IC2.log.debug(LogCategory.EnergyNet, "Restricting node %s to tier 20, requested %d.", this, tier);

				tier = 20;
			}
		}
		else
		{
			assert false;

			if (EnergyNetGlobal.debugGrid)
				IC2.log.warn(LogCategory.EnergyNet, "Node %s / te %s is using the invalid tier %d.", this, this.tile.entity, tier);

			tier = 0;
		}

		this.getTop().tier = tier;
	}

	double getAmount()
	{
		return this.getTop().amount;
	}

	void setAmount(double amount)
	{
		this.getTop().amount = amount;
	}

	double getResistance()
	{
		return this.getTop().resistance;
	}

	void setResistance(double resistance)
	{
		this.getTop().resistance = resistance;
	}

	double getVoltage()
	{
		return this.getTop().voltage;
	}

	void setVoltage(double voltage)
	{
		this.getTop().voltage = voltage;
	}

	double getMaxCurrent()
	{
		return this.tile.maxCurrent;
	}

	void resetCurrents()
	{
		this.getTop().currentIn = 0.0D;
		this.getTop().currentOut = 0.0D;
	}

	void addCurrent(double current)
	{
		if (current >= 0.0D)
		{
			Node var10000 = this.getTop();
			var10000.currentIn += current;
		}
		else
		{
			Node var3 = this.getTop();
			var3.currentOut += -current;
		}

	}

	public String toString()
	{
		String type = null;
		switch (this.nodeType)
		{
			case Source:
				type = "E";
				break;
			case Sink:
				type = "A";
				break;
			case Conductor:
				type = "C";
		}

		return this.tile.entity.getClass().getSimpleName().replace("TileEntity", "") + "|" + type + "|" + this.tier + "|" + this.uid;
	}

	Node getTop()
	{
		return this.parent != null ? this.parent.getTop() : this;
	}

	NodeLink getConnectionTo(Node node)
	{
		/* TODO gamerforEA code replace, old code:
		for (NodeLink link : this.links)
		{
			if (link.getNeighbor(this) == node)
				return link;
		} */
		List<NodeLink> links = this.links;
		for (int i = 0, linksSize = links.size(); i < linksSize; i++)
		{
			NodeLink link = links.get(i);
			if (link.getNeighbor(this) == node)
				return link;
		}
		// TODO gamerforEA code end

		return null;
	}

	NodeStats getStats()
	{
		return this.lastNodeStats;
	}

	void updateStats()
	{
		if (EnergyNetLocal.useLinearTransferModel)
			this.lastNodeStats.set(this.currentIn * this.voltage, this.currentOut * this.voltage, this.voltage);
		else
			this.lastNodeStats.set(this.currentIn, this.currentOut, this.voltage);

	}

	Grid getGrid()
	{
		return this.getTop().grid;
	}

	void setGrid(Grid grid)
	{
		if (grid == null)
			throw new NullPointerException("null grid");
		assert this.getTop().grid == null;

		this.getTop().grid = grid;
	}

	void clearGrid()
	{
		assert this.getTop().grid != null;

		this.getTop().grid = null;
	}
}
