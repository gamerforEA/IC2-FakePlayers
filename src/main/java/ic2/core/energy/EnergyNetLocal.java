package ic2.core.energy;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import com.gamerforea.ic2.EventConfig;

import ic2.api.Direction;
import ic2.api.energy.NodeStats;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.energy.tile.IMetaDelegate;
import ic2.core.IC2;
import ic2.core.TickHandler;
import ic2.core.init.MainConfig;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LogCategory;
import ic2.core.util.Util;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

public final class EnergyNetLocal
{
	public static final boolean useLinearTransferModel = ConfigUtil.getBool(MainConfig.get(), "misc/useLinearTransferModel");
	public static final double nonConductorResistance = 0.2D;
	public static final double sourceResistanceFactor = 0.0625D;
	public static final double sinkResistanceFactor = 1.0D;
	public static final double sourceCurrent = 17.0D;
	public static final boolean enableCache = true;
	private static int nextGridUid = 0;
	private static int nextNodeUid = 0;
	protected final Set<Grid> grids = new HashSet();
	protected List<Change> changes = new ArrayList();
	private final Map<ChunkCoordinates, Tile> registeredTiles = new HashMap();
	private Map<TileEntity, Integer> pendingAdds = new WeakHashMap();
	private final Set<Tile> removedTiles = new HashSet();
	private boolean locked = false;
	private static final long logSuppressionTimeout = 300000000000L;
	private final Map<String, Long> recentLogs = new HashMap();

	protected void addTileEntity(TileEntity te)
	{
		this.addTileEntity(te, 0);
	}

	protected void addTileEntity(TileEntity te, int retry)
	{
		if (EnergyNetGlobal.debugTileManagement)
			IC2.log.debug(LogCategory.EnergyNet, "EnergyNet.addTileEntity(%s, %d), world=%s, chunk=%s, this=%s", new Object[] { te, Integer.valueOf(retry), te.getWorldObj(), te.getWorldObj().getChunkFromBlockCoords(te.xCoord, te.zCoord), this });

		if (!(te instanceof IEnergyTile))
			this.logWarn("EnergyNet.addTileEntity: " + te + " doesn\'t implement IEnergyTile, aborting");
		else
		{
			if (EnergyNetGlobal.checkApi && !Util.checkInterfaces(te.getClass()))
				IC2.log.warn(LogCategory.EnergyNet, "EnergyNet.addTileEntity: %s doesn\'t implement its advertised interfaces completely.", new Object[] { Util.asString(te) });

			if (te.isInvalid())
				this.logWarn("EnergyNet.addTileEntity: " + te + " is invalid (TileEntity.isInvalid()), aborting");
			else if (te.getWorldObj() != DimensionManager.getWorld(te.getWorldObj().provider.dimensionId))
				this.logDebug("EnergyNet.addTileEntity: " + te + " is in an unloaded world, aborting");
			else if (this.locked)
			{
				this.logDebug("EnergyNet.addTileEntity: adding " + te + " while locked, postponing.");
				this.pendingAdds.put(te, Integer.valueOf(retry));
			}
			else
			{
				Tile tile = new Tile(this, te);
				if (EnergyNetGlobal.debugTileManagement)
				{
					List<String> posStrings = new ArrayList(tile.positions.size());

					for (TileEntity pos : tile.positions)
						posStrings.add(pos + " (" + pos.xCoord + "/" + pos.yCoord + "/" + pos.zCoord + ")");

					IC2.log.debug(LogCategory.EnergyNet, "positions: %s", new Object[] { posStrings });
				}

				ListIterator<TileEntity> it = tile.positions.listIterator();

				while (it.hasNext())
				{
					TileEntity pos = it.next();
					ChunkCoordinates coords = new ChunkCoordinates(pos.xCoord, pos.yCoord, pos.zCoord);
					Tile conflicting = this.registeredTiles.get(coords);
					if (conflicting != null)
					{
						if (te == conflicting.entity)
							this.logDebug("EnergyNet.addTileEntity: " + pos + " (" + te + ") is already added using the same position, aborting");
						else if (retry < 2)
							this.pendingAdds.put(te, Integer.valueOf(retry + 1));
						else if (!conflicting.entity.isInvalid() && !EnergyNetGlobal.replaceConflicting)
							this.logWarn("EnergyNet.addTileEntity: " + pos + " (" + te + ") is still conflicting with " + conflicting.entity + " using the same position (overlapping), aborting");
						else
						{
							this.logDebug("EnergyNet.addTileEntity: " + pos + " (" + te + ") is conflicting with " + conflicting.entity + " (invalid=" + conflicting.entity.isInvalid() + ") using the same position, which is abandoned (prev. te not removed), replacing");
							this.removeTileEntity(conflicting.entity);
							conflicting = null;
						}

						if (conflicting != null)
						{
							it.previous();

							while (it.hasPrevious())
							{
								pos = it.previous();
								coords = new ChunkCoordinates(pos.xCoord, pos.yCoord, pos.zCoord);
								this.registeredTiles.remove(coords);
							}

							return;
						}
					}

					if (!te.getWorldObj().blockExists(pos.xCoord, pos.yCoord, pos.zCoord))
					{
						if (retry < 1)
						{
							this.logWarn("EnergyNet.addTileEntity: " + pos + " (" + te + ") was added too early, postponing");
							this.pendingAdds.put(te, Integer.valueOf(retry + 1));
						}
						else
							this.logWarn("EnergyNet.addTileEntity: " + pos + " (" + te + ") unloaded, aborting");

						it.previous();

						while (it.hasPrevious())
						{
							pos = it.previous();
							coords = new ChunkCoordinates(pos.xCoord, pos.yCoord, pos.zCoord);
							this.registeredTiles.remove(coords);
						}

						return;
					}

					this.registeredTiles.put(coords, tile);

					for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
					{
						int x = pos.xCoord + dir.offsetX;
						int y = pos.yCoord + dir.offsetY;
						int z = pos.zCoord + dir.offsetZ;
						if (te.getWorldObj().blockExists(x, y, z))
							te.getWorldObj().notifyBlockOfNeighborChange(x, y, z, Blocks.air);
					}
				}

				this.addTileToGrids(tile);
				if (EnergyNetGlobal.verifyGrid())
					for (Node node : tile.nodes)
						assert node.getGrid() != null;

			}
		}
	}

	protected void removeTileEntity(TileEntity te)
	{
		if (this.locked)
			throw new IllegalStateException("removeTileEntity isn\'t allowed from this context");
		else
		{
			if (EnergyNetGlobal.debugTileManagement)
				IC2.log.debug(LogCategory.EnergyNet, "EnergyNet.removeTileEntity(%s), world=%s, chunk=%s, this=%s", new Object[] { te, te.getWorldObj(), te.getWorldObj().getChunkFromBlockCoords(te.xCoord, te.zCoord), this });

			if (!(te instanceof IEnergyTile))
				this.logWarn("EnergyNet.removeTileEntity: " + te + " doesn\'t implement IEnergyTile, aborting");
			else
			{
				List<TileEntity> positions;
				if (te instanceof IMetaDelegate)
					positions = ((IMetaDelegate) te).getSubTiles();
				else
					positions = Arrays.asList(new TileEntity[] { te });

				boolean wasPending = this.pendingAdds.remove(te) != null;
				if (EnergyNetGlobal.debugTileManagement)
				{
					List<String> posStrings = new ArrayList(positions.size());

					for (TileEntity pos : positions)
						posStrings.add(pos + " (" + pos.xCoord + "/" + pos.yCoord + "/" + pos.zCoord + ")");

					IC2.log.debug(LogCategory.EnergyNet, "positions: %s", new Object[] { posStrings });
				}

				boolean removed = false;

				for (TileEntity pos : positions)
				{
					ChunkCoordinates coords = new ChunkCoordinates(pos.xCoord, pos.yCoord, pos.zCoord);
					Tile tile = this.registeredTiles.get(coords);
					if (tile == null)
					{
						if (!wasPending)
							this.logDebug("EnergyNet.removeTileEntity: " + pos + " (" + te + ") wasn\'t found (added), skipping");
					}
					else if (tile.entity != te)
						this.logWarn("EnergyNet.removeTileEntity: " + pos + " (" + te + ") doesn\'t match the registered te " + tile.entity + ", skipping");
					else
					{
						if (!removed)
						{
							assert new HashSet(positions).equals(new HashSet(tile.positions));

							this.removeTileFromGrids(tile);
							removed = true;
							this.removedTiles.add(tile);
						}

						this.registeredTiles.remove(coords);
						if (te.getWorldObj().blockExists(pos.xCoord, pos.yCoord, pos.zCoord))
							for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
							{
								int x = pos.xCoord + dir.offsetX;
								int y = pos.yCoord + dir.offsetY;
								int z = pos.zCoord + dir.offsetZ;
								if (te.getWorldObj().blockExists(x, y, z))
									te.getWorldObj().notifyBlockOfNeighborChange(x, y, z, Blocks.air);
							}
					}
				}

			}
		}
	}

	protected double getTotalEnergyEmitted(TileEntity tileEntity)
	{
		ChunkCoordinates coords = new ChunkCoordinates(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
		Tile tile = this.registeredTiles.get(coords);
		if (tile == null)
		{
			this.logWarn("EnergyNet.getTotalEnergyEmitted: " + tileEntity + " is not added to the enet, aborting");
			return 0.0D;
		}
		else
		{
			double ret = 0.0D;

			for (NodeStats stat : tile.getStats())
				ret += stat.getEnergyOut();

			return ret;
		}
	}

	protected double getTotalEnergySunken(TileEntity tileEntity)
	{
		ChunkCoordinates coords = new ChunkCoordinates(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
		Tile tile = this.registeredTiles.get(coords);
		if (tile == null)
		{
			this.logWarn("EnergyNet.getTotalEnergySunken: " + tileEntity + " is not added to the enet, aborting");
			return 0.0D;
		}
		else
		{
			double ret = 0.0D;

			for (NodeStats stat : tile.getStats())
				ret += stat.getEnergyIn();

			return ret;
		}
	}

	protected NodeStats getNodeStats(TileEntity te)
	{
		ChunkCoordinates coords = new ChunkCoordinates(te.xCoord, te.yCoord, te.zCoord);
		Tile tile = this.registeredTiles.get(coords);
		if (tile == null)
		{
			this.logWarn("EnergyNet.getTotalEnergySunken: " + te + " is not added to the enet, aborting");
			return new NodeStats(0.0D, 0.0D, 0.0D);
		}
		else
		{
			double in = 0.0D;
			double out = 0.0D;
			double voltage = 0.0D;

			for (NodeStats stat : tile.getStats())
			{
				in += stat.getEnergyIn();
				out += stat.getEnergyOut();
				voltage = Math.max(voltage, stat.getVoltage());
			}

			return new NodeStats(in, out, voltage);
		}
	}

	protected TileEntity getTileEntity(int x, int y, int z)
	{
		Tile ret = this.registeredTiles.get(new ChunkCoordinates(x, y, z));
		return ret == null ? null : ret.entity;
	}

	protected TileEntity getNeighbor(TileEntity te, Direction dir)
	{
		switch (dir)
		{
			case XN:
				return this.getTileEntity(te.xCoord - 1, te.yCoord, te.zCoord);
			case XP:
				return this.getTileEntity(te.xCoord + 1, te.yCoord, te.zCoord);
			case YN:
				return this.getTileEntity(te.xCoord, te.yCoord - 1, te.zCoord);
			case YP:
				return this.getTileEntity(te.xCoord, te.yCoord + 1, te.zCoord);
			case ZN:
				return this.getTileEntity(te.xCoord, te.yCoord, te.zCoord - 1);
			case ZP:
				return this.getTileEntity(te.xCoord, te.yCoord, te.zCoord + 1);
			default:
				return null;
		}
	}

	public boolean dumpDebugInfo(PrintStream console, PrintStream chat, int x, int y, int z)
	{
		Tile tile = this.registeredTiles.get(new ChunkCoordinates(x, y, z));
		if (tile == null)
			return false;
		else
		{
			Set<Grid> processedGrids = new HashSet();

			for (Node node : tile.nodes)
			{
				Grid grid = node.getGrid();
				if (processedGrids.add(grid))
				{
					grid.dumpNodeInfo(chat, true, node);
					grid.dumpStats(chat, true);
					grid.dumpMatrix(console, true, true, true);
					console.println("dumping graph for " + grid);
					grid.dumpGraph(true);
				}
			}

			return true;
		}
	}

	public List<GridInfo> getGridInfos()
	{
		List<GridInfo> ret = new ArrayList();

		for (Grid grid : this.grids)
			ret.add(grid.getInfo());

		return ret;
	}

	// TODO gamerforEA code start
	private boolean skipTick = false;
	// TODO gamerforEA code end

	protected void onTickEnd()
	{
		if (IC2.platform.isSimulating())
		{
			// TODO gamerforEA code start
			if (EventConfig.skipTicks)
				if (this.skipTick)
				{
					this.skipTick = false;
					return;
				}
				else
					this.skipTick = true;
			// TODO gamerforEA code end

			this.locked = true;

			for (Grid grid : this.grids)
			{
				grid.finishCalculation();
				grid.updateStats();
			}

			this.locked = false;
			this.processChanges();
			Map<TileEntity, Integer> currentPendingAdds = this.pendingAdds;
			this.pendingAdds = new WeakHashMap();

			for (Entry<TileEntity, Integer> entry : currentPendingAdds.entrySet())
				this.addTileEntity(entry.getKey(), entry.getValue().intValue());

			this.locked = true;

			for (Grid grid : this.grids)
				grid.prepareCalculation();

			List<Runnable> tasks = new ArrayList();

			for (Grid grid : this.grids)
			{
				Runnable task = grid.startCalculation();
				if (task != null)
					tasks.add(task);
			}

			IC2.getInstance().threadPool.executeAll(tasks);
			this.locked = false;
		}
	}

	protected void addChange(Node node, ForgeDirection dir, double amount, double voltage)
	{
		this.changes.add(new Change(node, dir, amount, voltage));
	}

	protected static int getNextGridUid()
	{
		return nextGridUid++;
	}

	protected static int getNextNodeUid()
	{
		return nextNodeUid++;
	}

	private void addTileToGrids(Tile tile)
	{
		List<Node> extraNodes = new ArrayList();

		for (Node node : tile.nodes)
		{
			if (EnergyNetGlobal.debugGrid)
				IC2.log.debug(LogCategory.EnergyNet, "Adding node %s.", new Object[] { node });

			List<Node> neighbors = new ArrayList();

			for (TileEntity pos : tile.positions)
				for (Direction dir : Direction.directions)
				{
					ForgeDirection fdir = dir.toForgeDirection();
					ChunkCoordinates coords = new ChunkCoordinates(pos.xCoord + fdir.offsetX, pos.yCoord + fdir.offsetY, pos.zCoord + fdir.offsetZ);
					Tile neighborTile = this.registeredTiles.get(coords);
					if (neighborTile != null && neighborTile != node.tile)
						for (Node neighbor : neighborTile.nodes)
							if (!neighbor.isExtraNode())
							{
								boolean canEmit = false;
								if ((node.nodeType == NodeType.Source || node.nodeType == NodeType.Conductor) && neighbor.nodeType != NodeType.Source)
								{
									IEnergyEmitter emitter = (IEnergyEmitter) (pos instanceof IEnergyEmitter ? pos : node.tile.entity);
									TileEntity neighborSubTe = neighborTile.getSubEntityAt(coords);
									IEnergyAcceptor acceptor = (IEnergyAcceptor) (neighborSubTe instanceof IEnergyAcceptor ? neighborSubTe : neighbor.tile.entity);
									canEmit = emitter.emitsEnergyTo(neighbor.tile.entity, dir.toForgeDirection()) && acceptor.acceptsEnergyFrom(node.tile.entity, dir.getInverse().toForgeDirection());
								}

								boolean canAccept = false;
								if (!canEmit && (node.nodeType == NodeType.Sink || node.nodeType == NodeType.Conductor) && neighbor.nodeType != NodeType.Sink)
								{
									IEnergyAcceptor acceptor = (IEnergyAcceptor) (pos instanceof IEnergyAcceptor ? pos : node.tile.entity);
									TileEntity neighborSubTe = neighborTile.getSubEntityAt(coords);
									IEnergyEmitter emitter = (IEnergyEmitter) (neighborSubTe instanceof IEnergyEmitter ? neighborSubTe : neighbor.tile.entity);
									canAccept = acceptor.acceptsEnergyFrom(neighbor.tile.entity, dir.toForgeDirection()) && emitter.emitsEnergyTo(node.tile.entity, dir.getInverse().toForgeDirection());
								}

								if (canEmit || canAccept)
									neighbors.add(neighbor);
							}
				}

			if (neighbors.isEmpty())
			{
				if (EnergyNetGlobal.debugGrid)
					IC2.log.debug(LogCategory.EnergyNet, "Creating new grid for %s.", new Object[] { node });

				Grid grid = new Grid(this);
				grid.add(node, neighbors);
			}
			else
				switch (node.nodeType)
				{
					case Conductor:
						Grid grid = null;

						for (Node neighbor : neighbors)
							if (neighbor.nodeType == NodeType.Conductor || neighbor.links.isEmpty())
							{
								if (EnergyNetGlobal.debugGrid)
									IC2.log.debug(LogCategory.EnergyNet, "Using %s for %s with neighbors %s.", new Object[] { neighbor.getGrid(), node, neighbors });

								grid = neighbor.getGrid();
								break;
							}

						if (grid == null)
						{
							if (EnergyNetGlobal.debugGrid)
								IC2.log.debug(LogCategory.EnergyNet, "Creating new grid for %s with neighbors %s.", new Object[] { node, neighbors });

							grid = new Grid(this);
						}

						Map<Node, Node> neighborReplacements = new HashMap();
						ListIterator<Node> it = neighbors.listIterator();

						while (it.hasNext())
						{
							Node neighbor = it.next();
							if (neighbor.getGrid() != grid)
								if (neighbor.nodeType != NodeType.Conductor && !neighbor.links.isEmpty())
								{
									boolean found = false;

									for (int i = 0; i < it.previousIndex(); ++i)
									{
										Node neighbor2 = neighbors.get(i);
										if (neighbor2.tile == neighbor.tile && neighbor2.nodeType == neighbor.nodeType && neighbor2.getGrid() == grid)
										{
											if (EnergyNetGlobal.debugGrid)
												IC2.log.debug(LogCategory.EnergyNet, "Using neighbor node %s instead of %s.", new Object[] { neighbor2, neighbors });

											found = true;
											it.set(neighbor2);
											break;
										}
									}

									if (!found)
									{
										if (EnergyNetGlobal.debugGrid)
											IC2.log.debug(LogCategory.EnergyNet, "Creating new extra node for neighbor %s.", new Object[] { neighbor });

										neighbor = new Node(this, neighbor.tile, neighbor.nodeType);
										neighbor.tile.addExtraNode(neighbor);
										List<Node> empty = Collections.emptyList();
										grid.add(neighbor, empty);
										it.set(neighbor);

										assert neighbor.getGrid() != null;
									}
								}
								else
									grid.merge(neighbor.getGrid(), neighborReplacements);
						}

						it = neighbors.listIterator();

						while (it.hasNext())
						{
							Node neighbor = it.next();
							Node replacement = neighborReplacements.get(neighbor);
							if (replacement != null)
							{
								neighbor = replacement;
								it.set(replacement);
							}

							assert neighbor.getGrid() == grid;
						}

						grid.add(node, neighbors);

						assert node.getGrid() != null;
						break;
					case Sink:
					case Source:
						List<List<Node>> neighborGroups = new ArrayList();

						for (Node neighbor : neighbors)
						{
							boolean found = false;
							if (node.nodeType == NodeType.Conductor)
								for (List<Node> nodeList : neighborGroups)
								{
									Node neighbor2 = nodeList.get(0);
									if (neighbor2.nodeType == NodeType.Conductor && neighbor2.getGrid() == neighbor.getGrid())
									{
										nodeList.add(neighbor);
										found = true;
										break;
									}
								}

							if (!found)
							{
								List<Node> nodeList = new ArrayList();
								nodeList.add(neighbor);
								neighborGroups.add(nodeList);
							}
						}

						if (EnergyNetGlobal.debugGrid)
							IC2.log.debug(LogCategory.EnergyNet, "Neighbor groups detected for %s: %s.", new Object[] { node, neighborGroups });

						assert !neighborGroups.isEmpty();

						for (int i = 0; i < ((List) neighborGroups).size(); ++i)
						{
							List<Node> nodeList = neighborGroups.get(i);
							Node neighbor = nodeList.get(0);
							if (neighbor.nodeType != NodeType.Conductor && !neighbor.links.isEmpty())
							{
								assert nodeList.size() == 1;

								if (EnergyNetGlobal.debugGrid)
									IC2.log.debug(LogCategory.EnergyNet, "Creating new extra node for neighbor %s.", new Object[] { neighbor });

								neighbor = new Node(this, neighbor.tile, neighbor.nodeType);
								neighbor.tile.addExtraNode(neighbor);
								List<Node> empty = Collections.emptyList();
								new Grid(this).add(neighbor, empty);
								nodeList.set(0, neighbor);

								assert neighbor.getGrid() != null;
							}

							Node currentNode;
							if (i == 0)
								currentNode = node;
							else
							{
								if (EnergyNetGlobal.debugGrid)
									IC2.log.debug(LogCategory.EnergyNet, "Creating new extra node for %s.", new Object[] { node });

								currentNode = new Node(this, tile, node.nodeType);
								currentNode.setExtraNode(true);
								extraNodes.add(currentNode);
							}

							neighbor.getGrid().add(currentNode, nodeList);

							assert currentNode.getGrid() != null;
						}
				}
		}

		for (Node node : extraNodes)
			tile.addExtraNode(node);

	}

	private void removeTileFromGrids(Tile tile)
	{
		for (Node node : tile.nodes)
			node.getGrid().remove(node);

	}

	private void processChanges()
	{
		for (Tile tile : this.removedTiles)
		{
			Iterator<Change> it = this.changes.iterator();

			while (it.hasNext())
			{
				Change change = it.next();
				if (change.node.tile == tile)
				{
					Tile replacement = this.registeredTiles.get(new ChunkCoordinates(change.node.tile.entity.xCoord, change.node.tile.entity.yCoord, change.node.tile.entity.zCoord));
					boolean validReplacement = false;
					if (replacement != null)
						for (Node node : replacement.nodes)
							if (node.nodeType == change.node.nodeType && node.getGrid() == change.node.getGrid())
							{
								if (EnergyNetGlobal.debugGrid)
									IC2.log.debug(LogCategory.EnergyNet, "Redirecting change %s to replacement node %s.", new Object[] { change, node });

								change.node = node;
								validReplacement = true;
								break;
							}

					if (!validReplacement)
					{
						it.remove();
						List<Change> sameGridSourceChanges = new ArrayList();

						for (Change change2 : this.changes)
							if (change2.node.nodeType == NodeType.Source && change.node.getGrid() == change2.node.getGrid())
								sameGridSourceChanges.add(change2);

						if (EnergyNetGlobal.debugGrid)
							IC2.log.debug(LogCategory.EnergyNet, "Redistributing change %s to remaining source nodes %s.", new Object[] { change, sameGridSourceChanges });

						for (Change change2 : sameGridSourceChanges)
							change2.setAmount(change2.getAmount() - Math.abs(change.getAmount()) / sameGridSourceChanges.size());
					}
				}
			}
		}

		this.removedTiles.clear();

		for (Change change : this.changes)
			if (change.node.nodeType == NodeType.Sink)
			{
				assert change.getAmount() > 0.0D;

				IEnergySink sink = (IEnergySink) change.node.tile.entity;
				double returned = sink.injectEnergy(change.dir, change.getAmount(), change.getVoltage());
				if (EnergyNetGlobal.debugGrid)
					IC2.log.debug(LogCategory.EnergyNet, "Applied change %s, %f EU returned.", new Object[] { change, Double.valueOf(returned) });

				if (returned > 0.0D)
				{
					List<Change> sameGridSourceChanges = new ArrayList();

					for (Change change2 : this.changes)
						if (change2.node.nodeType == NodeType.Source && change.node.getGrid() == change2.node.getGrid())
							sameGridSourceChanges.add(change2);

					if (EnergyNetGlobal.debugGrid)
						IC2.log.debug(LogCategory.EnergyNet, "Redistributing returned amount to source nodes %s.", new Object[] { sameGridSourceChanges });

					for (Change change2 : sameGridSourceChanges)
						change2.setAmount(change2.getAmount() - returned / sameGridSourceChanges.size());
				}
			}

		for (Change change : this.changes)
			if (change.node.nodeType == NodeType.Source)
			{
				assert change.getAmount() <= 0.0D;

				if (change.getAmount() < 0.0D)
				{
					IEnergySource source = (IEnergySource) change.node.tile.entity;
					source.drawEnergy(change.getAmount());
					if (EnergyNetGlobal.debugGrid)
						IC2.log.debug(LogCategory.EnergyNet, "Applied change %s.", new Object[] { change });
				}
			}

		this.changes.clear();
	}

	private void logDebug(String msg)
	{
		if (this.shouldLog(msg))
		{
			IC2.log.debug(LogCategory.EnergyNet, msg);
			if (EnergyNetGlobal.debugTileManagement)
			{
				IC2.log.debug(LogCategory.EnergyNet, new Throwable(), "stack trace");
				if (TickHandler.getLastDebugTrace() != null)
					IC2.log.debug(LogCategory.EnergyNet, TickHandler.getLastDebugTrace(), "parent stack trace");
			}

		}
	}

	private void logWarn(String msg)
	{
		if (this.shouldLog(msg))
		{
			IC2.log.warn(LogCategory.EnergyNet, msg);
			if (EnergyNetGlobal.debugTileManagement)
			{
				IC2.log.debug(LogCategory.EnergyNet, new Throwable(), "stack trace");
				if (TickHandler.getLastDebugTrace() != null)
					IC2.log.debug(LogCategory.EnergyNet, TickHandler.getLastDebugTrace(), "parent stack trace");
			}

		}
	}

	private boolean shouldLog(String msg)
	{
		if (EnergyNetGlobal.logAll)
			return true;
		else
		{
			this.cleanRecentLogs();
			msg = msg.replaceAll("@[0-9a-f]+", "@x");
			long time = System.nanoTime();
			Long lastLog = this.recentLogs.put(msg, Long.valueOf(time));
			return lastLog == null || lastLog.longValue() < time - 300000000000L;
		}
	}

	private void cleanRecentLogs()
	{
		if (this.recentLogs.size() >= 100)
		{
			long minTime = System.nanoTime() - 300000000000L;
			Iterator<Long> it = this.recentLogs.values().iterator();

			while (it.hasNext())
			{
				long recTime = it.next().longValue();
				if (recTime < minTime)
					it.remove();
			}

		}
	}
}
