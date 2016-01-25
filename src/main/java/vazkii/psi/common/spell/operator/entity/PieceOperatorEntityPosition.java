/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Psi Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 * 
 * Psi is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 * 
 * File Created @ [18/01/2016, 21:50:51 (GMT)]
 */
package vazkii.psi.common.spell.operator.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import vazkii.psi.api.internal.Vector3;
import vazkii.psi.api.spell.Spell;
import vazkii.psi.api.spell.SpellContext;
import vazkii.psi.api.spell.SpellParam;
import vazkii.psi.api.spell.SpellRuntimeException;
import vazkii.psi.api.spell.param.ParamEntity;
import vazkii.psi.api.spell.piece.PieceOperator;

public class PieceOperatorEntityPosition extends PieceOperator {

	SpellParam target;
	
	public PieceOperatorEntityPosition(Spell spell) {
		super(spell);
	}
	
	@Override
	public void initParams() {
		addParam(target = new ParamEntity(SpellParam.GENERIC_NAME_TARGET, SpellParam.YELLOW, false, false));
	}
	
	@Override
	public Object execute(SpellContext context) throws SpellRuntimeException {
		Entity e = this.<Entity>getParamValue(context, target);
		
		if(e == null)
			throw new SpellRuntimeException(SpellRuntimeException.NULL_TARGET);
		
		Vector3 vec = Vector3.fromEntity(e);
		if(e instanceof EntityPlayer)
			vec.add(0, e.getEyeHeight(), 0);
		
		return vec;
	}
	
	@Override
	public Class<?> getEvaluationType() {
		return Vector3.class;
	}

}