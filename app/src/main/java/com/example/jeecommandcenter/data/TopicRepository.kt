package com.example.jeecommandcenter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ChapterTopic(val id: String, val chapterId: String, val title: String, val completed: Boolean)

class TopicRepository(context: Context) {
    private val prefs = context.getSharedPreferences("jee_topics", Context.MODE_PRIVATE)

    fun topicsFor(chapter: JeeChapter): List<ChapterTopic> {
        val canonicalId = JeeCatalog.normalizeChapterId(chapter.id)
        val savedById = read(canonicalId).associateBy { it.id }
        return defaultTopics(chapter).map { topic ->
            // Titles come from the current catalog; only completion state is user data.
            topic.copy(completed = savedById[topic.id]?.completed ?: false)
        }
    }

    fun setCompleted(chapterId: String, topicId: String, completed: Boolean) {
        val canonicalId = JeeCatalog.normalizeChapterId(chapterId)
        val chapter = JeeCatalog.find(canonicalId)
        val savedById = read(canonicalId).associateBy { it.id }
        val canonicalTopics = chapter?.let(::defaultTopics).orEmpty()
        val updated = if (canonicalTopics.isNotEmpty()) {
            canonicalTopics.map { topic ->
                topic.copy(
                    completed = if (topic.id == topicId) completed
                    else savedById[topic.id]?.completed ?: topic.completed
                )
            }
        } else {
            read(canonicalId).map { topic ->
                if (topic.id == topicId) topic.copy(completed = completed) else topic
            }
        }
        write(canonicalId, updated)
    }

    private fun read(chapterId: String): List<ChapterTopic> {
        val raw = prefs.getString("chapter_$chapterId", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                ChapterTopic(o.getString("id"), chapterId, o.getString("title"), o.optBoolean("completed"))
            }
        }.getOrDefault(emptyList())
    }

    private fun write(chapterId: String, topics: List<ChapterTopic>) {
        val array = JSONArray()
        topics.forEach {
            array.put(JSONObject().put("id", it.id).put("title", it.title).put("completed", it.completed))
        }
        prefs.edit().putString("chapter_$chapterId", array.toString()).apply()
    }

    private fun defaultTopics(chapter: JeeChapter): List<ChapterTopic> {
        val canonicalId = JeeCatalog.normalizeChapterId(chapter.id)
        val titles = chapterTopics[canonicalId] ?: listOf(
            "${chapter.name}: definitions and core principles",
            "${chapter.name}: laws, properties and formulas",
            "${chapter.name}: standard problem-solving methods",
            "${chapter.name}: graphs, cases and representations",
            "${chapter.name}: JEE Main applications",
            "${chapter.name}: JEE Advanced problem practice"
        )
        return titles.mapIndexed { index, title ->
            ChapterTopic("${canonicalId}_${index}", canonicalId, title, false)
        }
    }

    private val chapterTopics: Map<String, List<String>> = mapOf(
        "physics_1" to listOf(
            "SI base and derived units",
            "Dimensions and dimensional analysis",
            "Measurement errors and propagation",
            "Significant figures and rounding",
            "Vernier calipers and screw gauge",
            "Experimental skills and uncertainty"
        ),
        "physics_2" to listOf(
            "Position, distance and displacement",
            "Speed and velocity",
            "Acceleration and equations of motion",
            "Position–time and velocity–time graphs",
            "Uniformly accelerated motion",
            "Relative motion in one dimension",
            "Free-fall motion and graph problems"
        ),
        "physics_3" to listOf(
            "Vectors and vector components",
            "Projectile motion",
            "Relative motion in two dimensions",
            "Uniform circular motion",
            "Vector addition in kinematics",
            "River–boat and rain problems"
        ),
        "physics_4" to listOf(
            "Newton’s laws of motion",
            "Free-body diagrams and constraint forces",
            "Static and kinetic friction",
            "Tension and connected bodies",
            "Circular motion applications",
            "Pseudo force and accelerating frames"
        ),
        "physics_5" to listOf(
            "Work by constant and variable forces",
            "Work–energy theorem",
            "Kinetic and potential energy",
            "Conservation of mechanical energy",
            "Power and instantaneous power",
            "Collisions and coefficient of restitution",
            "Work from force–displacement graphs"
        ),
        "physics_6" to listOf(
            "Centre of mass of systems",
            "Motion of the centre of mass",
            "Linear momentum and conservation",
            "Impulse and impulsive forces",
            "Elastic and inelastic collisions",
            "Centre-of-mass energy and variable systems"
        ),
        "physics_7" to listOf(
            "Angular kinematics",
            "Torque and angular momentum",
            "Moment of inertia",
            "Parallel and perpendicular axis theorems",
            "Rolling motion without slipping",
            "Rotational energy and angular momentum conservation"
        ),
        "physics_8" to listOf(
            "Newton’s law of gravitation",
            "Gravitational field and acceleration",
            "Gravitational potential and potential energy",
            "Kepler’s laws and orbital motion",
            "Satellites and orbital energy",
            "Escape speed and weightlessness"
        ),
        "physics_9" to listOf(
            "Stress, strain and Hooke’s law",
            "Young’s, bulk and shear moduli",
            "Stress–strain curve",
            "Elastic potential energy",
            "Poisson’s ratio",
            "Elasticity of materials and applications"
        ),
        "physics_10" to listOf(
            "Fluid pressure and hydrostatics",
            "Pascal’s law and buoyancy",
            "Equation of continuity",
            "Bernoulli’s theorem and applications",
            "Viscosity, Stokes’ law and terminal speed",
            "Surface tension and capillarity"
        ),
        "physics_11" to listOf(
            "Thermal expansion of solids and liquids",
            "Calorimetry and specific heat",
            "Latent heat and phase changes",
            "Conduction, convection and radiation",
            "Newton’s law of cooling",
            "Combined heat-transfer problems"
        ),
        "physics_12" to listOf(
            "Thermal equilibrium and zeroth law",
            "Heat, work and internal energy",
            "First law of thermodynamics",
            "Thermodynamic processes and P–V diagrams",
            "Second law and heat engines",
            "Carnot engine, refrigerator and efficiency"
        ),
        "physics_13" to listOf(
            "Ideal-gas model and assumptions",
            "Pressure and molecular kinetic energy",
            "RMS, mean and most probable speeds",
            "Degrees of freedom",
            "Equipartition of energy",
            "Specific heats and mean free path"
        ),
        "physics_14" to listOf(
            "Simple harmonic motion and its equation",
            "Phase, frequency and time period",
            "Energy in SHM",
            "Mass–spring systems",
            "Simple pendulum",
            "Damped and forced oscillations; resonance"
        ),
        "physics_15" to listOf(
            "Progressive waves and wave equation",
            "Superposition and reflection",
            "Standing waves in strings and pipes",
            "Beats and frequency analysis",
            "Doppler effect",
            "Sound intensity, resonance and harmonics"
        ),
        "physics_16" to listOf(
            "Charge and Coulomb’s law",
            "Electric field and field lines",
            "Electric flux",
            "Gauss’s law",
            "Electric dipole and dipole field",
            "Applications of Gauss’s law"
        ),
        "physics_17" to listOf(
            "Electric potential and potential difference",
            "Potential due to point charges and systems",
            "Equipotential surfaces",
            "Capacitance and capacitor combinations",
            "Dielectrics and polarization",
            "Energy stored in a capacitor"
        ),
        "physics_18" to listOf(
            "Electric current and current density",
            "Drift velocity and mobility",
            "Ohm’s law and V–I characteristics",
            "Resistance, resistivity and temperature",
            "Kirchhoff’s laws and circuit analysis",
            "Cells, emf and internal resistance",
            "Wheatstone bridge, potentiometer and power"
        ),
        "physics_19" to listOf(
            "Lorentz force on moving charges",
            "Motion of charges in electric and magnetic fields",
            "Biot–Savart law and field of a current",
            "Ampere’s circuital law",
            "Force between parallel currents",
            "Torque on a current loop and cyclotron"
        ),
        "physics_20" to listOf(
            "Bar magnet and magnetic dipole",
            "Magnetic field lines and torque",
            "Earth’s magnetism",
            "Diamagnetic, paramagnetic and ferromagnetic materials",
            "Magnetisation and susceptibility",
            "Hysteresis and magnetic materials"
        ),
        "physics_21" to listOf(
            "Magnetic flux and Faraday’s law",
            "Lenz’s law and conservation of energy",
            "Motional emf",
            "Self and mutual inductance",
            "Energy stored in an inductor",
            "Eddy currents and AC generators"
        ),
        "physics_22" to listOf(
            "Sinusoidal voltage, current and RMS values",
            "Inductive and capacitive reactance",
            "Series LCR circuits",
            "Resonance in AC circuits",
            "Power factor and average power",
            "Transformers and AC power"
        ),
        "physics_23" to listOf(
            "Displacement current and Maxwell’s idea",
            "Properties of electromagnetic waves",
            "Electromagnetic wave propagation",
            "Electromagnetic spectrum",
            "Polarisation and wave nature",
            "Energy, intensity and applications"
        ),
        "physics_24" to listOf(
            "Reflection and spherical mirrors",
            "Refraction and Snell’s law",
            "Total internal reflection",
            "Thin lenses and lens-maker’s formula",
            "Prisms and dispersion",
            "Optical instruments and lens combinations"
        ),
        "physics_25" to listOf(
            "Huygens’ principle and wavefronts",
            "Interference and Young’s double-slit experiment",
            "Path difference and fringe width",
            "Diffraction and single-slit pattern",
            "Polarisation of light",
            "Resolving power and optical limits"
        ),
        "physics_26" to listOf(
            "Photoelectric effect and observations",
            "Einstein’s photoelectric equation",
            "de Broglie wavelength",
            "Matter waves and momentum relations",
            "Davisson–Germer experiment",
            "Wave–particle duality numericals"
        ),
        "physics_27" to listOf(
            "Rutherford model and atomic structure",
            "Bohr model and hydrogen atom",
            "Energy levels and spectral series",
            "Transitions between energy states",
            "Atomic spectra and line wavelengths",
            "Bohr radius, velocity and energy calculations"
        ),
        "physics_28" to listOf(
            "Nuclear composition, size and density",
            "Mass defect and binding energy",
            "Radioactive decay law",
            "Half-life and mean life",
            "Nuclear reactions and Q-value",
            "Nuclear fission and fusion"
        ),
        "physics_29" to listOf(
            "Energy bands and semiconductor types",
            "Intrinsic and extrinsic semiconductors",
            "p–n junction and diode characteristics",
            "Rectifiers and filtering",
            "Zener diode and voltage regulation",
            "Logic gates and digital electronics"
        ),
        "chemistry_1" to listOf(
            "Measurements, SI units and significant figures",
            "Mole concept and molar mass",
            "Empirical and molecular formulae",
            "Stoichiometry and balanced equations",
            "Limiting reagent, yield and purity",
            "Concentration terms and mixtures",
            "Percentage composition and related calculations"
        ),
        "chemistry_2" to listOf(
            "Atomic models and subatomic particles",
            "Bohr model and hydrogen spectrum",
            "Quantisation and energy levels",
            "de Broglie wavelength",
            "Heisenberg uncertainty principle",
            "Quantum numbers and orbitals",
            "Electronic configuration and atomic spectra"
        ),
        "chemistry_3" to listOf(
            "Modern periodic law and periodic table",
            "Electronic configuration and element position",
            "Atomic and ionic radii",
            "Ionisation enthalpy trends",
            "Electron gain enthalpy and electronegativity",
            "Valency and periodic property trends"
        ),
        "chemistry_4" to listOf(
            "Lewis structures and octet rule",
            "Formal charge and resonance",
            "VSEPR theory and molecular shape",
            "Hybridisation and orbital overlap",
            "Molecular orbital theory",
            "Bond parameters and polarity",
            "Hydrogen bonding"
        ),
        "chemistry_5" to listOf(
            "System, surroundings and state functions",
            "First law of thermodynamics",
            "Enthalpy and enthalpy changes",
            "Entropy and spontaneity",
            "Gibbs energy and equilibrium",
            "Thermochemical calculations and Hess’s law"
        ),
        "chemistry_6" to listOf(
            "Chemical equilibrium and equilibrium constant",
            "Kc, Kp and their relationship",
            "Le Chatelier’s principle",
            "Acids, bases and pH",
            "Buffers and salt hydrolysis",
            "Solubility product and common-ion effect"
        ),
        "chemistry_7" to listOf(
            "Oxidation number rules",
            "Oxidising and reducing agents",
            "Balancing redox equations",
            "Redox reactions in acidic and basic media",
            "Disproportionation and comproportionation",
            "Redox equivalents and titrations",
            "Redox applications in electrochemistry"
        ),
        "chemistry_8" to listOf(
            "IUPAC nomenclature and structural representation",
            "Structural and stereoisomerism",
            "Inductive, resonance and hyperconjugation effects",
            "Reaction intermediates and stability",
            "Organic reaction mechanisms and types",
            "Purification and qualitative analysis"
        ),
        "chemistry_9" to listOf(
            "Alkanes: preparation and reactions",
            "Alkenes: addition and oxidation reactions",
            "Alkynes and acidic character",
            "Aromaticity and structure of benzene",
            "Electrophilic aromatic substitution",
            "Hydrocarbon reactions and product prediction"
        ),
        "chemistry_10" to listOf(
            "Concentration units and solubility",
            "Ideal and non-ideal solutions",
            "Raoult’s law and vapour pressure",
            "Colligative properties",
            "van ’t Hoff factor and association",
            "Molar-mass determination and numericals"
        ),
        "chemistry_11" to listOf(
            "Galvanic and electrolytic cells",
            "Electrode potential and cell emf",
            "Nernst equation and equilibrium",
            "Conductivity and molar conductivity",
            "Faraday’s laws of electrolysis",
            "Batteries, fuel cells and corrosion"
        ),
        "chemistry_12" to listOf(
            "Rate of reaction and rate laws",
            "Order and molecularity",
            "Integrated rate equations",
            "Half-life and reaction completion",
            "Arrhenius equation and activation energy",
            "Catalysts and reaction mechanisms"
        ),
        "chemistry_13" to listOf(
            "Adsorption and adsorption isotherms",
            "Physisorption and chemisorption",
            "Catalysis and catalytic activity",
            "Colloids, sols and emulsions",
            "Coagulation and colloid properties",
            "Surface area and practical applications"
        ),
        "chemistry_14" to listOf(
            "Group 13 and 14 elements",
            "Group 15 elements and compounds",
            "Group 16 elements and compounds",
            "Group 17 elements and halogens",
            "Group 18 elements and noble gases",
            "Periodic trends, oxides and anomalous behaviour"
        ),
        "chemistry_15" to listOf(
            "Electronic configuration and transition series",
            "Oxidation states and periodic trends",
            "Colour, complex formation and magnetism",
            "Lanthanoid contraction and f-block trends",
            "KMnO₄: preparation and reactions",
            "K₂Cr₂O₇: preparation and reactions"
        ),
        "chemistry_16" to listOf(
            "Coordination terminology and nomenclature",
            "Structural and stereoisomerism",
            "Valence bond theory",
            "Crystal-field splitting and d-orbitals",
            "Magnetic moment and colour",
            "Stability, chelation and applications"
        ),
        "chemistry_17" to listOf(
            "C–X bond, classification and preparation",
            "Nucleophilic substitution: SN1 mechanism",
            "Nucleophilic substitution: SN2 mechanism",
            "Elimination reactions and competing pathways",
            "Reactions of haloarenes",
            "Named reactions and environmental effects"
        ),
        "chemistry_18" to listOf(
            "Preparation and reactions of alcohols",
            "Oxidation and dehydration of alcohols",
            "Phenol acidity and electrophilic substitution",
            "Ether synthesis and Williamson reaction",
            "Cleavage and reactions of ethers",
            "Distinguishing tests for alcohols and phenols"
        ),
        "chemistry_19" to listOf(
            "Carbonyl structure and reactivity",
            "Nucleophilic addition reactions",
            "Oxidation and reduction of carbonyl compounds",
            "Aldol condensation and Cannizzaro reaction",
            "Carboxylic-acid acidity and derivatives",
            "Named reactions and identification tests"
        ),
        "chemistry_20" to listOf(
            "Classification and basicity of amines",
            "Preparation of primary, secondary and tertiary amines",
            "Reactions of amines with electrophiles",
            "Diazonium salts and their reactions",
            "Azo coupling and substitution reactions",
            "Identification and distinction tests"
        ),
        "chemistry_21" to listOf(
            "Carbohydrates and their reactions",
            "Amino acids and protein structure",
            "Enzymes and biological catalysis",
            "Vitamins and deficiency disorders",
            "Nucleic acids: DNA and RNA",
            "Monomers, polymers and polymerisation"
        ),
        "chemistry_22" to listOf(
            "Qualitative salt analysis",
            "Tests for organic functional groups",
            "Detection of nitrogen, sulphur and halogens",
            "Titrations and quantitative analysis",
            "Chromatography and separation methods",
            "Purification methods and laboratory safety"
        ),
        "mathematics_1" to listOf(
            "Types of sets, subsets and power sets",
            "Union, intersection and set operations",
            "Complements and De Morgan’s laws",
            "Cartesian products and ordered pairs",
            "Set cardinality and number of subsets",
            "Inclusion–exclusion principle",
            "Set-based counting and word problems"
        ),
        "mathematics_2" to listOf(
            "Relations, domain and range",
            "Types of relations and equivalence",
            "Domain, codomain and range of functions",
            "One-one, onto and bijective functions",
            "Composition and inverse of functions",
            "Binary operations and function graphs"
        ),
        "mathematics_3" to listOf(
            "Angles, radians and arc length",
            "Trigonometric identities and formulae",
            "Graphs, period and amplitude",
            "Transformations and compound angles",
            "Trigonometric equations",
            "Inverse trigonometric functions"
        ),
        "mathematics_4" to listOf(
            "Algebra of complex numbers",
            "Argand plane and geometric representation",
            "Modulus and argument",
            "Polar form and De Moivre’s theorem",
            "Conjugate and geometric properties",
            "Roots of complex equations"
        ),
        "mathematics_5" to listOf(
            "Roots and coefficients",
            "Discriminant and nature of roots",
            "Relations between roots",
            "Quadratic inequalities",
            "Parameter-based quadratic equations",
            "Graphical interpretation of quadratics",
            "Common roots and transformations"
        ),
        "mathematics_6" to listOf(
            "Arithmetic, geometric and harmonic progressions",
            "Nth term and sum of AP and GP",
            "Arithmetic and geometric means",
            "Telescoping and special series",
            "Sums of powers and standard series",
            "Infinite geometric series and convergence"
        ),
        "mathematics_7" to listOf(
            "Fundamental counting principle and factorials",
            "Permutations and arrangements",
            "Restricted and circular permutations",
            "Combinations and selection",
            "Distribution and grouping problems",
            "Permutation–combination identities and applications"
        ),
        "mathematics_8" to listOf(
            "Binomial expansion and general term",
            "Middle term and term independent of x",
            "Binomial coefficients and their properties",
            "Greatest term and coefficient comparison",
            "Sums of coefficients and binomial identities",
            "Approximations and binomial applications"
        ),
        "mathematics_9" to listOf(
            "Slope and equations of a straight line",
            "Angle between two lines",
            "Distance of a point from a line",
            "Family of lines and parameter form",
            "Collinearity and concurrency",
            "Intersections and angle bisectors"
        ),
        "mathematics_10" to listOf(
            "Circle equation, centre and radius",
            "Tangents and normals to a circle",
            "Chords, secants and polar lines",
            "Length of tangent from an external point",
            "Intersections and relative position of circles",
            "Family of circles and common chords"
        ),
        "mathematics_11" to listOf(
            "Parabola: standard forms and properties",
            "Ellipse: foci, axes and eccentricity",
            "Hyperbola and its asymptotes",
            "Focus, directrix and eccentricity",
            "Tangents and normals to conics",
            "Intersections and common chord of conics"
        ),
        "mathematics_12" to listOf(
            "Limit laws and algebra of limits",
            "Standard limits and recognised forms",
            "Algebraic simplification and factorisation",
            "Trigonometric limits",
            "Exponential and logarithmic limits",
            "One-sided limits and existence"
        ),
        "mathematics_13" to listOf(
            "Continuity at a point",
            "Continuity on intervals",
            "Differentiability and non-differentiable points",
            "Derivative rules and basic derivatives",
            "Chain, product and quotient rules",
            "Implicit and logarithmic differentiation",
            "Higher derivatives and inverse-trig derivatives"
        ),
        "mathematics_14" to listOf(
            "Monotonicity and intervals of increase/decrease",
            "Tangents and normals",
            "Increasing and decreasing functions",
            "Local and absolute maxima/minima",
            "Rolle’s theorem and mean value theorem",
            "Rates of change and approximations"
        ),
        "mathematics_15" to listOf(
            "Standard indefinite integrals",
            "Integration by substitution",
            "Integration by parts",
            "Partial fractions and rational functions",
            "Definite integrals and their properties",
            "Symmetry and definite-integral evaluation"
        ),
        "mathematics_16" to listOf(
            "Area under a curve",
            "Area between two curves",
            "Intersection points and region boundaries",
            "Symmetry in area calculations",
            "Setting integration limits correctly",
            "Areas of standard JEE curves and regions"
        ),
        "mathematics_17" to listOf(
            "Order, degree and formation of differential equations",
            "Formation of differential equations",
            "Variables-separable equations",
            "Homogeneous first-order equations",
            "First-order linear differential equations",
            "Initial conditions and applications"
        ),
        "mathematics_18" to listOf(
            "Matrix notation and types of matrices",
            "Matrix addition, subtraction and scalar multiplication",
            "Matrix multiplication and compatibility",
            "Transpose, symmetric and skew-symmetric matrices",
            "Determinant, adjoint and inverse",
            "Solving matrix equations"
        ),
        "mathematics_19" to listOf(
            "Evaluation of determinants",
            "Properties and simplification of determinants",
            "Minors and cofactors",
            "Adjoint and inverse of a matrix",
            "Area of a triangle using determinants",
            "Systems of linear equations",
            "Determinant-based solutions of linear systems"
        ),
        "mathematics_20" to listOf(
            "Vector components and magnitude",
            "Vector addition and scalar multiplication",
            "Dot product and angle between vectors",
            "Cross product and area",
            "Scalar triple product and volume",
            "Projection and vector geometry"
        ),
        "mathematics_21" to listOf(
            "Direction ratios and direction cosines",
            "Equations of lines in 3D",
            "Angle between lines",
            "Skew lines and shortest distance",
            "Planes, equations and distances",
            "Line–plane intersections and angles"
        ),
        "mathematics_22" to listOf(
            "Mean and measures of central tendency",
            "Variance and standard deviation",
            "Grouped data and frequency tables",
            "Combined mean and variance",
            "Standard-deviation calculations",
            "Statistical distribution numericals"
        ),
        "mathematics_23" to listOf(
            "Sample space and events",
            "Addition theorem of probability",
            "Conditional probability",
            "Bayes’ theorem",
            "Independent events",
            "Random variables and distributions"
        ),
        "mathematics_24" to listOf(
            "Statements, propositions and truth values",
            "Logical connectives",
            "Truth tables",
            "Converse, inverse and contrapositive",
            "Implication and logical equivalence",
            "Validity of arguments and set-based reasoning"
        )
    )
}
