import ReactECharts from 'echarts-for-react';

interface Props {
  data: Array<{ date: string; nav: number }>;
}

export default function EquityCurve({ data }: Props) {
  const hasData = data && data.length > 0;

  const option = {
    tooltip: {
      trigger: 'axis' as const,
      formatter: (params: { axisValue: string; value: number }[]) =>
        `${params[0].axisValue}<br/>净值: ${params[0].value.toFixed(2)}`,
    },
    xAxis: {
      type: 'category' as const,
      data: hasData ? data.map(d => d.date) : [],
    },
    yAxis: {
      type: 'value' as const,
      name: '净值',
    },
    series: [{
      name: '净值',
      type: 'line',
      data: hasData ? data.map(d => d.nav) : [],
      smooth: true,
      showSymbol: false,
      lineStyle: { color: '#1677ff', width: 2 },
      areaStyle: { color: 'rgba(22, 119, 255, 0.1)' },
    }],
    grid: { left: 60, right: 20, top: 20, bottom: 40 },
  };

  if (!hasData) {
    return <div style={{ height: 300, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#999' }}>
      暂无净值数据
    </div>;
  }

  return <ReactECharts option={option} style={{ height: 350 }} />;
}
