package game.model;

public class Avatar extends Unit {
    public Avatar(String id, String ownerId, String name, int hp, TilePos position) {
        // Avatar defaults: atk=2, moveRange=0, attackRange=1 (can be adjusted later)
        super(id, ownerId, name, 2, hp, 0, 1, position);
    }
}
