package dev.sidequest.xp;

/**
 * O regulă de acordare a XP-ului. {@link XpService} însumează toate strategiile înregistrate,
 * deci o regulă nouă (ex. bonus de weekend) e doar un @Component nou, fără modificări în service.
 */
public interface XpStrategy {

    /** Eticheta afișată în detaliul recompensei. */
    String name();

    /** XP-ul acordat de această regulă; 0 dacă nu se aplică. */
    int xpFor(XpContext context);
}
