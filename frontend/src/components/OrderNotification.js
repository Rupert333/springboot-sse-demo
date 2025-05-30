import React, {useState, useEffect, useCallback, useRef } from 'react';
import {Card, List, Tag, Button, message, Typography, Spin, Divider} from 'antd';
// import './OrderNotification.css';

const {Title, Text} = Typography;
const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080';
// 客户端ID，用于标识SSE连接
const clientId = `client-121113`;

/**
 * 订单通知组件
 * 使用SSE接收来自后端的实时订单更新通知
 */
const OrderNotification = () => {
  // 存储接收到的订单事件
  const [orderEvents, setOrderEvents] = useState([]);
  // SSE连接状态
  const [connected, setConnected] = useState(false);
  // 加载状态
  const [loading, setLoading] = useState(false);

  // 最后一次心跳时间
  const [lastHeartbeat, setLastHeartbeat] = useState(null);
  // 重连计时器
  const [reconnectTimer, setReconnectTimer] = useState(null);
  // 后端API地址
  const sseRef = useRef();

  const addListener = () => {
    // 连接建立时的处理
    sseRef.current?.addEventListener('open', async () => {
      console.log('SSE连接已建立');
      setConnected(true);
      setLoading(false);
      await message.success('已连接到订单通知服务');
    });

    // 接收到CONNECT事件的处理
    sseRef.current?.addEventListener('CONNECT', (event) => {
      console.log('收到连接成功事件:', event.data);
      setConnected(true);
      setLoading(false);
      setLastHeartbeat(new Date());
    });

    // 接收到HEARTBEAT事件的处理
    sseRef.current?.addEventListener('HEARTBEAT', (event) => {
      console.log('收到心跳消息:', event.data);
      setLastHeartbeat(new Date());
    });

    // 接收到ORDER_UPDATE事件的处理
    sseRef.current?.addEventListener('ORDER_UPDATE', async (event) => {
      try {
        // 解析接收到的JSON数据
        const orderEvent = JSON.parse(event.data);
        console.log('收到订单更新:', orderEvent);

        // 将新的订单事件添加到列表头部
        setOrderEvents(prevEvents => [orderEvent, ...prevEvents]);

        // 显示通知消息
        await message.info(`收到订单 ${orderEvent.orderId} 的状态更新: ${orderEvent.status}`);
      } catch (error) {
        console.error('解析订单事件失败:', error);
      }
    });

    // 连接错误处理
    sseRef.current?.addEventListener('error', (error) => {
      console.error('SSE连接错误:', error);

      // 关闭当前连接
      if (sseRef.current) {
        sseRef.current?.close();
      }

      setConnected(false);
      setLoading(false);

      // 避免频繁显示错误消息
      // if (!reconnectTimer) {
      //     message.error('订单通知服务连接失败，5秒后将重试');
      //
      //     // 5秒后尝试重连
      //     const timer = setTimeout(() => {
      //         console.log('尝试重新连接...');
      //         connectSSE();
      //     }, 5000);
      //
      //     setReconnectTimer(timer);
      // }
    });
  }

  /**
   * 连接SSE
   * 建立与后端的SSE连接，接收实时订单更新
   */
  const connectSSE = useCallback(() => {

    // 如果已经连接，先关闭之前的连接
    if (sseRef.current) {
      sseRef.current?.close();
    }

    // 清除可能存在的重连计时器
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      setReconnectTimer(null);
    }
    sseRef.current = new EventSource(`${apiUrl}/api/sse/connect?clientId=${clientId}`);
    addListener();
    setLoading(true);

  }, [reconnectTimer]);

  /**
   * 断开SSE连接
   */
  const disconnectSSE = useCallback(async () => {
    if (sseRef.current) {
      sseRef.current.close();
      sseRef.current = null;
      sseRef.current?.removeEventListener();
      setConnected(false);
      await message.info('已断开订单通知服务');
    }

    // 清除可能存在的重连计时器
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      setReconnectTimer(null);
    }
  }, [reconnectTimer]);

  /**
   * 模拟订单回调
   * 调用后端接口生成模拟订单事件
   */
  const simulateOrderCallback = async () => {
    try {
      const response = await fetch(`${apiUrl}/api/order/simulate`);
      if (response.ok) {
        message.success('已触发模拟订单回调');
      } else {
        message.error('触发模拟订单回调失败');
      }
    } catch (error) {
      console.error('模拟订单回调出错:', error);
      message.error('触发模拟订单回调失败');
    }
  };

  /**
   * 获取订单状态对应的标签颜色
   */
  const getStatusColor = (status) => {
    const statusColors = {
      'CREATED': 'blue',
      'PAID': 'green',
      'PROCESSING': 'orange',
      'SHIPPED': 'purple',
      'DELIVERED': 'success',
      'CANCELLED': 'red',
    };
    return statusColors[status] || 'default';
  };

  /**
   * 格式化时间戳
   */
  const formatTimestamp = (timestamp) => {
    if (!timestamp) return '';
    try {
      const date = new Date(timestamp);
      return date.toLocaleString();
    } catch (e) {
      return timestamp;
    }
  };

  /**
   * 检查心跳状态
   * 如果超过45秒没有收到心跳，尝试重新连接
   */
  useEffect(() => {
    if (!connected || !lastHeartbeat) return;

    const heartbeatCheckInterval = setInterval(() => {
      const now = new Date();
      const timeSinceLastHeartbeat = now - lastHeartbeat;

      // 如果超过45秒没有收到心跳，尝试重新连接
      if (timeSinceLastHeartbeat > 45000) {
        console.warn('超过45秒未收到心跳，尝试重新连接');
        message.warning('连接可能已断开，正在重新连接...');

        // 重新连接
        if (sseRef.current) {
          sseRef.current?.close();
        }
        connectSSE();
      }
    }, 10000); // 每10秒检查一次

    return () => {
      clearInterval(heartbeatCheckInterval);
    };
  }, [connected, lastHeartbeat]);

  return (
      <div className="order-notification-container">
        <Card
            title={
              <div className="card-title">
                <Title level={4}>订单实时通知</Title>
                <div className="connection-status">
                  <Text>状态: </Text>
                  {loading ? (
                      <Spin size="small"/>
                  ) : (
                      <Tag color={connected ? 'green' : 'red'}>
                        {connected ? '已连接' : '未连接'}
                      </Tag>
                  )}
                  {lastHeartbeat && connected && (
                      <Text type="secondary">
                        最后心跳: {new Date(lastHeartbeat).toLocaleTimeString()}
                      </Text>
                  )}
                </div>
              </div>
            }
            extra={
              <div className="card-actions">
                {connected ? (
                    <Button type="primary" danger onClick={disconnectSSE}>
                      断开连接
                    </Button>
                ) : (
                    <Button type="primary" onClick={connectSSE}>
                      连接
                    </Button>
                )}
                <Button
                    onClick={simulateOrderCallback}
                    disabled={!connected}
                    style={{marginLeft: '8px'}}
                >
                  模拟订单回调
                </Button>
              </div>
            }
        >
          <Divider orientation="left">订单更新历史</Divider>

          {orderEvents.length === 0 ? (
              <div className="empty-message">
                <Text type="secondary">暂无订单更新，请等待通知或点击"模拟订单回调"按钮测试</Text>
              </div>
          ) : (
              <List
                  dataSource={orderEvents}
                  renderItem={(item) => (
                      <List.Item>
                        <Card
                            className="order-event-card"
                            size="small"
                            title={`订单: ${item.orderId}`}
                            extra={<Tag color={getStatusColor(item.status)}>{item.status}</Tag>}
                        >
                          <p><strong>金额:</strong> ¥{item.amount?.toFixed(2)}</p>
                          <p><strong>时间:</strong> {formatTimestamp(item.timestamp)}</p>
                          {item.message && <p><strong>消息:</strong> {item.message}</p>}
                        </Card>
                      </List.Item>
                  )}
              />
          )}
        </Card>
      </div>
  );
};

export default OrderNotification;
