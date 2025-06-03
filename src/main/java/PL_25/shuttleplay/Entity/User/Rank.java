package PL_25.shuttleplay.Entity.User;

/*
* 배드민턴 급수 및 초기 mmr 값 지정을 위한 enum 클래스
* */

public enum Rank {
    SS("ss", 1900),
    S("s", 1600),
    A("a", 1300),
    B("b", 1000),
    C("c", 700),
    D("d", 400),
    E("e", 100);

    private final String label;
    private final int initialMmr;

    Rank(String label, int initialMmr) {
        this.label = label;
        this.initialMmr = initialMmr;
    }

    public int getInitialMmr() {
        return initialMmr;
    }

    public static Rank fromString(String rankStr) {
        for (Rank rank : Rank.values()) {
            if (rank.label.equalsIgnoreCase(rankStr)) {
                return rank;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 Rank: " + rankStr);
    }
}
