package com.fou.power

/**
 * Central power system constants and heat math.
 */
object PowerConstants {

    // ── Fuel ────────────────────────────────────────────────────────────────
    const val COAL_W_MIN       = 1f
    const val COAL_W_MAX       = 20f
    const val COAL_BLOCK_MULT  = 2f   // coal_max * 2
    const val GLOW_DUST_MULT   = 3f   // coal_block_max * 3
    const val GLOW_BLOCK_MULT  = 2f   // glow_dust_max * 2

    val COAL_BLOCK_W_MAX  = COAL_W_MAX * COAL_BLOCK_MULT        // 40W
    val GLOW_DUST_W_MAX   = COAL_BLOCK_W_MAX * GLOW_DUST_MULT   // 120W
    val GLOW_BLOCK_W_MAX  = GLOW_DUST_W_MAX * GLOW_BLOCK_MULT   // 240W

    // Burn duration range in ticks (5s-40s = 100-800 ticks)
    const val BURN_TICKS_MIN = 100
    const val BURN_TICKS_MAX = 800

    // ── Heat ─────────────────────────────────────────────────────────────────
    // Heat is stored 0-1000 internally; 1000 = 100% = BOOM
    const val HEAT_MAX           = 1000
    const val HEAT_EXPLODE       = 1000

    // Heat gain per tick when power has nowhere to go (scales with heat level)
    fun heatGainPerTick(currentHeat: Int, watts: Float): Float {
        val pct = currentHeat / HEAT_MAX.toFloat()
        val accel = when {
            pct < 0.30f -> 0.5f
            pct < 0.60f -> 1.5f
            pct < 0.80f -> 4.0f
            else        -> 10.0f
        }
        // Scale also by how much wattage is wasted
        return accel * (watts / GLOW_BLOCK_W_MAX) * 2f
    }

    // Passive cooling per tick when power IS being consumed
    const val HEAT_COOL_PER_TICK = 0.8f
}
