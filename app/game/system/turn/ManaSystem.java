package game.system.turn;

import game.model.Player;

public class ManaSystem {
    private final int manaCap;

    public ManaSystem(int manaCap) {
        this.manaCap = manaCap;
    }

    public int getManaCap() {
        return manaCap;
    }

    public <T> void refreshAtStartTurn(Player<T> player) {
        int newMax = Math.min(manaCap, player.getMaxMana() + 1);
        player.setMaxMana(newMax);
        player.setMana(newMax);
    }

    public <T> boolean canPay(Player<T> player, int cost) {
        return player.canPay(cost);
    }

    public <T> void spend(Player<T> player, int cost) {
        player.spendMana(cost);
    }
}
