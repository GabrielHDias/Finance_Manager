import ThemeToggle from "@/components/theme-toggle";
import Link from "next/link";
import HomeSessionAction from "@/components/home-session-action";
import styles from "./page.module.css";

const features = [
  { title: "Contas", description: "Organize suas contas financeiras e consulte o saldo de cada uma em um só lugar.", icon: "M3 7h18v13H3z M3 7l9-4 9 4 M7 11v5 M12 11v5 M17 11v5", number: "01" },
  { title: "Transações", description: "Registre receitas e despesas, organize por categoria e encontre movimentações com filtros.", icon: "M4 7h15 M15 3l4 4-4 4 M20 17H5 M9 13l-4 4 4 4", number: "02" },
  { title: "Visão financeira", description: "Acompanhe receitas, despesas e saldo por período para entender sua movimentação financeira.", icon: "M4 4v16h16 M8 16v-4 M13 16V8 M18 16V5", number: "03" },
];

function FeatureIcon({ path }: { path: string }) {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d={path} />
    </svg>
  );
}

export default function HomePage() {
  return (
    <div className={styles.page}>
      <a className={styles.skipLink} href="#conteudo">Pular para o conteúdo</a>
      <header className={styles.header}>
        <Link className={styles.brand} href="/" aria-label="Finance Manager — início">
          <span className={styles.brandMark} aria-hidden="true">F</span>
          Finance Manager
        </Link>
        <nav className={styles.navigation} aria-label="Navegação principal">
          <a className={styles.navLink} href="#funcionalidades">Funcionalidades</a>
          <ThemeToggle />
          <HomeSessionAction className={styles.primaryLink} />
        </nav>
      </header>
      <main id="conteudo" className={styles.main}>
        <section className={styles.hero} aria-labelledby="hero-title">
          <div>
            <p className={styles.eyebrow}>Seu dia a dia financeiro</p>
            <h1 id="hero-title" className={styles.title}>Suas finanças, de forma <span>simples e organizada.</span></h1>
            <p className={styles.description}>Organize suas contas, acompanhe receitas e despesas e tenha uma visão clara da sua vida financeira em um só lugar.</p>
            <div className={styles.actions}>
              <HomeSessionAction className={styles.primaryLink} />
              <a className={styles.secondaryLink} href="#funcionalidades">Conhecer funcionalidades <span aria-hidden="true">↓</span></a>
            </div>
            <p className={styles.signup}>Primeira vez por aqui? <Link href="/register">Criar uma conta</Link></p>
          </div>
          <div className={styles.overview} aria-label="Da organização das contas à visão financeira">
            <p className={styles.overviewHeading}>Tudo começa com organização</p>
            <ol className={styles.steps}>
              {features.map((feature) => (
                <li key={feature.title}>
                  <span className={styles.icon}><FeatureIcon path={feature.icon} /></span>
                  <span>{feature.title}</span>
                  <span className={styles.stepNumber} aria-hidden="true">{feature.number}</span>
                </li>
              ))}
            </ol>
            <p className={styles.overviewNote}>Do registro de cada movimentação à clareza sobre o seu saldo.</p>
          </div>
        </section>
        <section id="funcionalidades" className={styles.features} aria-labelledby="features-title">
          <div className={styles.sectionHeading}>
            <p className={styles.eyebrow}>O essencial, bem organizado</p>
            <h2 id="features-title">Mais clareza em cada movimentação.</h2>
            <p>Ferramentas simples para acompanhar suas finanças com consistência.</p>
          </div>
          <div className={styles.featureGrid}>
            {features.map((feature) => (
              <article key={feature.title} className={styles.featureCard}>
                <span className={styles.icon}><FeatureIcon path={feature.icon} /></span>
                <h3>{feature.title}</h3>
                <p>{feature.description}</p>
              </article>
            ))}
          </div>
        </section>
      </main>
      <footer className={styles.footer}>
        <div>
          <p className={styles.footerBrand}>Finance Manager</p>
          <p>Projeto open source de gerenciamento financeiro pessoal.</p>
        </div>
        <a className={styles.repository} href="https://github.com/GabrielHDias/Finance_Manager">Ver projeto no GitHub <span aria-hidden="true">↗</span></a>
      </footer>
    </div>
  );
}
