package com.games.tactics;

class Weapon extends Item
{
	public Weapon() {}

	/**
	 * Fires the weapon at a target at a specified distance.
	 * 
	 * @param inDistance The distance from the target. Affects accuracy.
	 * @return 0 for a miss, -1 if the target was invalid, or the damage done to the target.
	 */
	public int attack(int inDistance)
	{
		if (!canAttack(inDistance))
			return -1;
				
		double conditionDecrease = Math.random() - mDurability;
		if (conditionDecrease > 0)
			mCondition -= (conditionDecrease / 10.0);
		
		// should scale this by inDistance!
		if (Math.random() < mAccuracy) {
			return (int)((Math.random() * (mMaxDamage - mMinDamage)) + mMinDamage);
		} else
			return 0;
	}
	
	public boolean canAttack()
	{
		return mCondition > 0.0;
	}

	public boolean canAttack(int inDistance)
	{
		return canAttack() && Math.abs(inDistance) <= mRange;
	}
	
	protected int mRange; ///< Max range of weapon. Range of 1 is a melee weapon.
	protected int mMaxDamage; ///< Maximum damage of weapon.
	protected int mMinDamage; ///< Minimum damage of weapon.
	protected double mAccuracy; ///< Accuracy at max range. Increases as range decreases.
	protected double mCondition; ///< Decreases with use. 1 is brand new, 0 means it cannot be used.
	protected double mDurability; ///< The rate at which the condition decreases with use. Higher is more durable.
}

class ProjectileWeapon extends Weapon
{
	public ProjectileWeapon() {}
	
	public int attack(int inDistance)
	{
		int outDamage = super.attack(inDistance);
		
		if (outDamage > 0)
			mAmmoCount--;
		
		return outDamage;
	}
	
	public boolean canAttack()
	{
		return super.canAttack() && mAmmoCount > 0;
	}
	
	protected int mAmmoCapacity;
	protected int mAmmoCount;
}

class Pistol extends ProjectileWeapon
{
	public Pistol()
	{
		mRange = 5;
		mMaxDamage = 5;
		mMinDamage = 3;
		mCondition = 1.0;
		mDurability = 0.3;
		mAccuracy = 0.5;
		
		mAmmoCapacity = 9;
		mAmmoCount = 9;
	}
}