# DB Mapping Library Evaluation

Evaluation of database mapping/ORM libraries for SmecklesWebApp (Play 3.x + Scala 3 + H2 dev / PostgreSQL production).

## Requirements

- Map Scala case classes to relational tables
- Support 1→N relationships (Customer → ShoppingList → ShoppingListItem)
- Work with both H2 (dev) and PostgreSQL (production) via config swap
- Return `Future[T]` to fit Play's async model
- Integrate cleanly with Play Framework 3.x and Guice DI
- Idiomatic Scala (immutable case classes, functional composition)

## Comparison Matrix

| # | Library | GitHub Stars | Play Integration | Async Model | Scala 3 Support | Documentation Quality |
|---|---------|-------------|------------------|-------------|-----------------|----------------------|
| 1 | [Slick](https://github.com/slick/slick) | 2.7k | Official `play-slick` plugin | `Future[T]` native | ✅ Full | Excellent — book, API docs, examples |
| 2 | [ScalikeJDBC](https://github.com/scalikejdbc/scalikejdbc) | 1.3k | `scalikejdbc-play-support` | Blocking (async extension exists) | ✅ Full | Good — cookbook, website, examples |
| 3 | [Anorm](https://github.com/playframework/anorm) | ~240 | Built by Play team | Blocking (wrap in Future) | ✅ Full | Decent — covered in Play docs |
| 4 | [Doobie](https://github.com/typelevel/doobie) | 2.2k | None (Cats/http4s ecosystem) | Cats Effect `IO` | ✅ Full | Excellent microsite |
| 5 | [Quill (ZIO)](https://github.com/zio/zio-quill) | 2.2k | None (ZIO ecosystem) | ZIO Effect | Partial (protoquill) | Good but fragmented |
| 6 | [Hibernate/JPA](https://github.com/hibernate/hibernate-orm) | 6k+ | None for Play/Scala | Blocking | N/A (Java) | Massive but Java-centric |

## Detailed Assessment

### 1. Slick (Recommended)

**What it is:** Functional Relational Mapping (FRM) library — queries are Scala expressions compiled to SQL.

**Strengths:**
- Official Play plugin (`play-slick`) handles connection pool lifecycle, config binding, and evolutions
- Returns `Future[T]` natively — zero impedance mismatch with Play's async controllers
- Type-safe DSL: queries compose like Scala collections (`filter`, `map`, `join`)
- Case class ↔ table projection with no annotations or mutable state
- Same code runs against H2 and PostgreSQL — only `application.conf` changes
- Most referenced Scala DB library in Play projects across open source
- Active development (v3.6.1, May 2025)
- HikariCP connection pooling included via play-slick

**Weaknesses:**
- Steeper learning curve than raw SQL approaches
- Query DSL can be opaque for complex SQL (CTEs, window functions)
- Compile times increase with many table definitions

**1→N relationship support:** Foreign key definitions + `for`-comprehension joins or nested queries returning tuples mapped to case classes.

### 2. ScalikeJDBC

**What it is:** Thin JDBC wrapper — you write SQL strings but get type-safe result extraction.

**Strengths:**
- Simple mental model: write SQL, parse results with type-safe extractors
- Play support module available
- Supports H2 and PostgreSQL
- Good documentation with cookbook and website
- Lower abstraction = easier debugging of generated queries

**Weaknesses:**
- Primarily blocking — async extension (`scalikejdbc-async`) is less mature
- SQL strings are not compile-time checked
- More boilerplate for complex joins and 1→N mapping

### 3. Anorm

**What it is:** Play's built-in raw SQL library with row parsers.

**Strengths:**
- Zero additional dependencies (part of Play ecosystem)
- Full SQL control — no DSL to learn
- Row parsers compose for nested objects

**Weaknesses:**
- No type-safe query construction
- Verbose for 1→N relationships (manual join parsing)
- Blocking by default — must wrap in `Future { blocking { ... } }`
- Smaller community and fewer examples than Slick

### 4. Doobie (Not recommended for this stack)

**What it is:** Pure functional JDBC layer built on Cats Effect.

**Why not:** Built for the Typelevel ecosystem (http4s, Cats Effect `IO`). Integrating with Play's `Future`-based model requires `IO → Future` bridging which adds complexity and a runtime dependency (cats-effect) for no practical gain over Slick.

### 5. Quill / ZIO-Quill (Not recommended for this stack)

**What it is:** Compile-time query generation using Scala macros.

**Why not:** Tightly coupled to ZIO. Same ecosystem mismatch as Doobie. Scala 3 support is via `zio-protoquill` which is less mature (176 stars, separate repo).

### 6. Hibernate/JPA (Not recommended)

**What it is:** Java's dominant ORM framework.

**Why not:** Mutable entity beans, annotation-heavy, fights against Scala's immutable case classes and functional patterns. No Play integration. Would require a Java-style architecture that contradicts the project's design.

## Decision

**Slick** is the clear choice for this project:

1. Only library with first-party Play integration (`play-slick`)
2. `Future[T]` return type matches existing controller/service async pattern
3. Type-safe 1→N joins for Customer → ShoppingList → ShoppingListItem
4. H2/PostgreSQL portability via config-only swap
5. Largest adoption in Play + Scala projects

### Dependencies to add

```scala
// build.sbt
libraryDependencies ++= Seq(
  "org.playframework" %% "play-slick"            % "6.1.1",
  "org.playframework" %% "play-slick-evolutions" % "6.1.1",
  "org.postgresql"     % "postgresql"            % "42.7.3"  // production driver
)
```

### Configuration (already partially in place)

```hocon
# conf/application.conf — Slick replaces raw jdbc module
slick.dbs.default.profile = "slick.jdbc.H2Profile$"
slick.dbs.default.db.driver = "org.h2.Driver"
slick.dbs.default.db.url = "jdbc:h2:mem:shoppinglist;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
slick.dbs.default.db.user = "sa"
slick.dbs.default.db.password = ""
```

```hocon
# conf/production.conf
slick.dbs.default.profile = "slick.jdbc.PostgresProfile$"
slick.dbs.default.db.driver = "org.postgresql.Driver"
slick.dbs.default.db.url = ${DB_URL}
slick.dbs.default.db.user = ${DB_USERNAME}
slick.dbs.default.db.password = ${DB_PASSWORD}
```

---

*Evaluated: 2026-05-26*
