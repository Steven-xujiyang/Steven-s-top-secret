import { useEffect, useState } from 'react';
import { Card, Col, Row, Statistic, Table, Tag, Button, Space } from 'antd';
import { ReloadOutlined, PlayCircleOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { backtestApi, BacktestResponse } from '../../api/backtestApi';

export default function DashboardPage() {
  const [jobs, setJobs] = useState<BacktestResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => { fetchJobs(); }, []);

  const fetchJobs = async () => {
    setLoading(true);
    try {
      const { data } = await backtestApi.listJobs();
      setJobs(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error('Failed to load jobs', e);
    } finally {
      setLoading(false);
    }
  };

  const statusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'green';
      case 'RUNNING': return 'blue';
      case 'FAILED': return 'red';
      case 'CANCELLED': return 'orange';
      default: return 'default';
    }
  };

  const columns = [
    { title: 'Job ID', dataIndex: 'jobId', key: 'jobId', width: 100 },
    { title: '策略', dataIndex: 'strategyName', key: 'strategyName' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => <Tag color={statusColor(s)}>{s}</Tag> },
    { title: '信息', dataIndex: 'message', key: 'message', ellipsis: true },
    {
      title: '操作', key: 'actions',
      render: (_: unknown, record: BacktestResponse) => (
        <Space>
          {record.status === 'COMPLETED' && (
            <Button size="small" onClick={() => navigate(`/results/${record.jobId}`)}>查看</Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Row justify="space-between" style={{ marginBottom: 16 }}>
        <Col><h2>回测概览</h2></Col>
        <Col>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={fetchJobs}>刷新</Button>
            <Button type="primary" icon={<PlayCircleOutlined />} onClick={() => navigate('/backtest')}>
              新建回测
            </Button>
          </Space>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}><Card><Statistic title="总回测数" value={jobs.length} /></Card></Col>
        <Col span={6}>
          <Card><Statistic title="已完成" value={jobs.filter(j => j.status === 'COMPLETED').length} valueStyle={{ color: '#3f8600' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="运行中" value={jobs.filter(j => j.status === 'RUNNING').length} valueStyle={{ color: '#1677ff' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="失败" value={jobs.filter(j => j.status === 'FAILED').length} valueStyle={{ color: '#cf1322' }} /></Card>
        </Col>
      </Row>

      <Card title="回测任务列表">
        <Table columns={columns} dataSource={jobs} rowKey="jobId" loading={loading} size="middle" />
      </Card>
    </div>
  );
}
