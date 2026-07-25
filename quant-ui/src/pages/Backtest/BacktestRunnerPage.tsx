import { useState } from 'react';
import { Card, Form, Input, Select, DatePicker, InputNumber, Button, Alert, Space, message } from 'antd';
import { PlayCircleOutlined } from '@ant-design/icons';
import { backtestApi } from '../../api/backtestApi';

const { RangePicker } = DatePicker;

export default function BacktestRunnerPage() {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<{ jobId: string; status: string } | null>(null);

  const onFinish = async (values: Record<string, unknown>) => {
    setLoading(true);
    setResult(null);
    try {
      const req = {
        strategyClass: values.strategyClass as string,
        symbols: (values.symbols as string).split(/[,，\s]+/).filter(Boolean),
        startDate: (values.dateRange as [dayjs.Dayjs, dayjs.Dayjs])[0].format('YYYY-MM-DD'),
        endDate: (values.dateRange as [dayjs.Dayjs, dayjs.Dayjs])[1].format('YYYY-MM-DD'),
        initialCapital: values.initialCapital as number,
        frequency: values.frequency as string,
        params: values.params ? JSON.parse(values.params as string) : {},
      };
      const { data } = await backtestApi.submit(req);
      setResult(data);
      message.success('回测任务已提交');
    } catch (e) {
      message.error('提交失败: ' + (e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>新建回测</h2>
      <Card style={{ maxWidth: 700 }}>
        <Form form={form} layout="vertical" onFinish={onFinish}
          initialValues={{
            strategyClass: 'DualMovingAverageStrategy',
            symbols: '600036, 000001, 600519',
            initialCapital: 1000000,
            frequency: 'day',
          }}
        >
          <Form.Item label="策略" name="strategyClass" rules={[{ required: true }]}>
            <Select options={[
              { value: 'DualMovingAverageStrategy', label: '双均线策略 (DualMA)' },
              { value: 'MeanReversionStrategy', label: '均值回归策略' },
              { value: 'FactorRankingStrategy', label: '多因子排序策略' },
            ]} />
          </Form.Item>

          <Form.Item label="股票代码" name="symbols" rules={[{ required: true }]}
            tooltip="多个代码用逗号分隔">
            <Input placeholder="600036, 000001, 600519" />
          </Form.Item>

          <Form.Item label="回测区间" name="dateRange" rules={[{ required: true }]}>
            <RangePicker style={{ width: '100%' }} />
          </Form.Item>

          <Space size="large">
            <Form.Item label="初始资金" name="initialCapital">
              <InputNumber min={10000} step={100000} style={{ width: 180 }}
                formatter={(value) => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')} />
            </Form.Item>

            <Form.Item label="频率" name="frequency">
              <Select style={{ width: 100 }} options={[
                { value: 'day', label: '日线' }, { value: 'week', label: '周线' },
              ]} />
            </Form.Item>
          </Space>

          <Form.Item label="策略参数 (JSON)" name="params"
            tooltip="可选，JSON格式的策略参数">
            <Input placeholder='{"shortPeriod": 5, "longPeriod": 20}' />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading}
              icon={<PlayCircleOutlined />} size="large">
              开始回测
            </Button>
          </Form.Item>
        </Form>

        {result && (
          <Alert type="success" message="任务已提交"
            description={`Job ID: ${result.jobId} — 状态: ${result.status}`} showIcon />
        )}
      </Card>
    </div>
  );
}
