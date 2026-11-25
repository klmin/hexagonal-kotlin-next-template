import { Component, type ErrorInfo, type ReactNode } from "react";

import { Button } from "@/components/ui/button";

type State = { error: unknown };

// 렌더링 중 예상하지 못한 오류가 나면 화면 전체가 비지 않게 오류 화면을 보여 준다.
// 백엔드 오류처럼 예상 가능한 오류는 각 화면에서 메시지로 보여 주고 여기까지 오지 않는다
export class ErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: unknown): State {
    return { error };
  }

  componentDidCatch(error: unknown, info: ErrorInfo) {
    console.error(error, info.componentStack);
  }

  render() {
    if (this.state.error === null) {
      return this.props.children;
    }
    return (
      <main className="mx-auto flex w-full max-w-xl flex-col gap-4 px-4 py-12">
        <h1 className="text-xl font-semibold">문제가 발생했습니다</h1>
        <p className="text-sm text-muted-foreground">잠시 후 다시 시도해 주세요.</p>
        <Button type="button" onClick={() => this.setState({ error: null })} className="self-start">
          다시 시도
        </Button>
      </main>
    );
  }
}
