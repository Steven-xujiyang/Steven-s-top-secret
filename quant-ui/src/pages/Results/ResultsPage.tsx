import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Card, Descriptions, Row, Col, Spin, Alert, Statistic } from 'antd';
import { backtestApi, BacktestResponse } from '../../api/backtestApi';
import EquityCurve from './components/EquityCurve';

export default function ResultsPage() {
  const { jobId } = useParams<{ jobId: string }>();
  const [data, setData] = useState<BacktestResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!jobId) return;
    (async () => {
      setLoading(true);
      try {
        const { data } = await backtestApi.getResult(jobId);
        setData(data);
      } catch (e) {
        console.error('Failed to load result', e);
      } finally {
        setLoading(false);
      }
    })();
  }, [jobId]);

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />;
  if (!data) return <Alert type="error" message="未找到回测结果" />;

  const m = data.metrics || {};

  return (
    <div>
      <h2>回测结果 — {data.strategyName || jobId}</h2>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}><Card><Statistic title="总收益" value={((m.totalReturn || 0) * 100).toFixed(2)} suffix="%" /></Card></Col>
        <Col span={6}><Card><Statistic title="年化收益" value={((m.annualReturn || 0) * 100).toFixed(2)} suffix="%" /></Card></Col>
        <Col span={6}><Card><Statistic title="夏普比率" value={(m.sharpeRatio || 0).toFixed(2)} /></Card></Col>
        <Col span={6}><Card><Statistic title="最大回撤" value={((m.maxDrawdown || 0) * 100).toFixed(2)} suffix="%" valueStyle={{ color: '#cf1322' }} /></Card></Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}><Card><Statistic title="年化波动" value={((m.annualVolatility || 0) * 100).toFixed(2)} suffix="%" /></Card></Col>
        <Col span={6}><Card><Statistic title="Calmar比率" value={(m.calmarRatio || 0).toFixed(2)} /></Card></Col>
        <Col span={6}><Card><Statistic title="胜率" value={((m.winRate || 0) * 100).toFixed(1)} suffix="%" /></Card></Col>
        <Col span={6}><Card><Statistic title="交易次数" value={m.totalTrades || 0} /></Card></Col>
      </Row>

      <Card title="净值曲线" style={{ marginBottom: 16 }}>
        <EquityCurve data={data.navCurve || []} />
      </Card>

      <Card title="详细指标">
        <Descriptions column={2} bordered size="small">
          {Object.entries(m).map(([key, value]) => (
            <Descriptions.Item key={key} label={key}>
              {typeof value === 'number' ? value.toFixed(4) : String(value)}
            </Descriptions.Item>
          ))}
        </Descriptions>
      </Card>
    </div>
  );
}
