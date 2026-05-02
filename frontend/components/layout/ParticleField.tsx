const PARTICLE_COUNT = 18;

export function ParticleField() {
  return (
    <div className="particle-field" aria-hidden="true">
      {Array.from({ length: PARTICLE_COUNT }).map((_, index) => (
        <span key={index} />
      ))}
    </div>
  );
}
