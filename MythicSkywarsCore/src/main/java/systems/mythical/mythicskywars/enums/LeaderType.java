package systems.mythical.mythicskywars.enums;

public enum LeaderType {
    WINS,
    LOSSES,
    KILLS,
    DEATHS,
    XP;

    public static LeaderType matchType(String string) {
        for (LeaderType type : LeaderType.values()) {
            if (type.toString().equalsIgnoreCase(string)) {
                return type;
            }
        }
        return null;
    }
}