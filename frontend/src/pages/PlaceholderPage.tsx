type PlaceholderPageProps = {
  title: string;
  description: string;
};

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-8 shadow-sm">
      <span className="inline-flex rounded-full bg-amber-100 px-2 py-1 text-xs font-medium text-amber-800">Upcoming Phase</span>
      <h2 className="mb-2 mt-3 text-2xl font-semibold">{title}</h2>
      <p className="text-slate-600">{description}</p>
    </section>
  );
}
