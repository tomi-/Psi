/*
 * This class is distributed as a part of the Psi Mod.
 * Get the Source Code on GitHub:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.spell.trick.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.fluids.IFluidBlock;

import vazkii.psi.api.PsiAPI;
import vazkii.psi.api.internal.Vector3;
import vazkii.psi.api.spell.EnumSpellStat;
import vazkii.psi.api.spell.Spell;
import vazkii.psi.api.spell.SpellCompilationException;
import vazkii.psi.api.spell.SpellContext;
import vazkii.psi.api.spell.SpellMetadata;
import vazkii.psi.api.spell.SpellParam;
import vazkii.psi.api.spell.SpellRuntimeException;
import vazkii.psi.api.spell.param.ParamVector;
import vazkii.psi.api.spell.piece.PieceTrick;

public class PieceTrickBreakBlock extends PieceTrick {

	SpellParam<Vector3> position;

	public PieceTrickBreakBlock(Spell spell) {
		super(spell);
	}

	@Override
	public void initParams() {
		addParam(position = new ParamVector(SpellParam.GENERIC_NAME_POSITION, SpellParam.BLUE, false, false));
	}

	@Override
	public void addToMetadata(SpellMetadata meta) throws SpellCompilationException {
		super.addToMetadata(meta);

		meta.addStat(EnumSpellStat.POTENCY, 20);
		meta.addStat(EnumSpellStat.COST, 50);
	}

	@Override
	public Object execute(SpellContext context) throws SpellRuntimeException {
		ItemStack tool = context.getHarvestTool();
		Vector3 positionVal = this.getParamValue(context, position);

		if (positionVal == null) {
			throw new SpellRuntimeException(SpellRuntimeException.NULL_VECTOR);
		}
		if (!context.isInRadius(positionVal)) {
			throw new SpellRuntimeException(SpellRuntimeException.OUTSIDE_RADIUS);
		}

		BlockPos pos = positionVal.toBlockPos();
		removeBlockWithDrops(context, context.caster, context.caster.getEntityWorld(), tool, pos, true);

		return null;
	}

	public static void removeBlockWithDrops(SpellContext context, PlayerEntity player, World world, ItemStack tool, BlockPos pos, boolean particles) {
		if (!world.isBlockLoaded(pos) || (context.positionBroken != null && pos.equals(new BlockPos(context.positionBroken.getHitVec().x, context.positionBroken.getHitVec().y, context.positionBroken.getHitVec().z))) || !world.isBlockModifiable(player, pos)) {
			return;
		}

		if (tool.isEmpty()) {
			tool = PsiAPI.getPlayerCAD(player);
		}

		BlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if (!block.isAir(state, world, pos) && !(block instanceof IFluidBlock) && state.getBlockHardness(world, pos) != -1) {
			if (!canHarvestBlock(block, player, world, pos, tool)) {
				return;
			}

			BreakEvent event = createBreakEvent(state, player, world, pos, tool);
			MinecraftForge.EVENT_BUS.post(event);
			if (!event.isCanceled()) {
				if (!player.abilities.isCreativeMode) {
					TileEntity tile = world.getTileEntity(pos);

					if (block.removedByPlayer(state, world, pos, player, true, world.getFluidState(pos))) {
						block.onPlayerDestroy(world, pos, state);
						block.harvestBlock(world, player, pos, state, tile, tool);
						block.dropXpOnBlockBreak(world, pos, event.getExpToDrop());
					}
				} else {
					world.removeBlock(pos, false);
				}
			}

			if (particles) {
				world.playEvent(2001, pos, Block.getStateId(state));
			}
		}
	}

	// Based on BreakEvent::new, allows a tool that isn't your mainhand tool to harvest the blocks
	public static BreakEvent createBreakEvent(BlockState state, PlayerEntity player, World world, BlockPos pos, ItemStack tool) {
		BreakEvent event = new BreakEvent(world, pos, state, player);
		if (state == null || !ForgeHooks.canHarvestBlock(state, player, world, pos)) // Handle empty block or player unable to break block scenario
		{
			event.setExpToDrop(0);
		} else {
			int bonusLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool);
			int silklevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, tool);
			event.setExpToDrop(state.getExpDrop(world, pos, bonusLevel, silklevel));
		}
		return event;
	}

	// Based on InventoryPlayer::canHarvestBlock
	public static boolean canHarvestBlock(BlockState state, ItemStack itemstack) {
		if(state.getMaterial().isToolNotRequired())
			return true;
		else
			return !itemstack.isEmpty() && itemstack.canHarvestBlock(state);
	}

	// Based on ForgeHooks::canHarvestBlock and EntityPlayer::canHarvestBlock
	public static boolean canHarvestBlock(Block block, PlayerEntity player, World world, BlockPos pos, ItemStack stack) {
		BlockState state = world.getBlockState(pos);
		if(state.getMaterial().isToolNotRequired())
			return true;

		// ItemStack stack = player.getHeldItemMainhand();
		ToolType tool = block.getHarvestTool(state);
		if(stack.isEmpty() || tool == null)
			// return player.canHarvestBlock(state);
			return ForgeEventFactory.doPlayerHarvestCheck(player, state, canHarvestBlock(state, stack));

		int toolLevel = stack.getItem().getHarvestLevel(stack, tool, player, state);
		if(toolLevel < 0)
			// return player.canHarvestBlock(state);
			return ForgeEventFactory.doPlayerHarvestCheck(player, state, canHarvestBlock(state, stack));

		return toolLevel >= block.getHarvestLevel(state);
	}
}
