type PlaceholderPageProps = {
  title: string;
  description: string;
};

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-8 shadow-sm">
      <h2 className="mb-2 text-2xl font-semibold">{title}</h2>
      <p className="text-slate-600">{description}</p>
    </section>
  );
}