import { PageHeader } from "../components/ui/PageHeader";
import { Panel } from "../components/ui/Panel";

type PlaceholderPageProps = {
  title: string;
  breadcrumb: string;
};

export function PlaceholderPage({ title, breadcrumb }: PlaceholderPageProps) {
  return (
    <section>
      <PageHeader title={title} breadcrumb={breadcrumb} />
      <Panel className="h-[28rem] bg-surface" />
    </section>
  );
}
